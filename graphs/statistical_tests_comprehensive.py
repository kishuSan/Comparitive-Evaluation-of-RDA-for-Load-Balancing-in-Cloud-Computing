"""
Statistical Comparison with Effect Size Analysis
Addresses: Comment 3 (Statistical Validation) and Comment 9 (Analytical Interpretation)
"""

import pandas as pd
import numpy as np
from scipy.stats import ttest_ind, wilcoxon, mannwhitneyu, ranksums
from scipy import stats
import itertools
import os

def calculate_cohens_d(group1, group2):
    """
    Calculate Cohen's d effect size.
    
    Interpretation:
    - |d| < 0.2: negligible
    - 0.2 ≤ |d| < 0.5: small
    - 0.5 ≤ |d| < 0.8: medium
    - |d| ≥ 0.8: large
    """
    n1, n2 = len(group1), len(group2)
    var1, var2 = np.var(group1, ddof=1), np.var(group2, ddof=1)
    pooled_std = np.sqrt(((n1-1)*var1 + (n2-1)*var2) / (n1+n2-2))
    
    if pooled_std == 0:
        return 0.0
    
    return (np.mean(group1) - np.mean(group2)) / pooled_std


def interpret_effect_size(d):
    """Interpret Cohen's d value"""
    abs_d = abs(d)
    if abs_d < 0.2:
        return "Negligible"
    elif abs_d < 0.5:
        return "Small"
    elif abs_d < 0.8:
        return "Medium"
    else:
        return "Large"


def calculate_rank_biserial(group1, group2):
    """
    Calculate rank-biserial correlation (effect size for Wilcoxon/Mann-Whitney).
    
    Interpretation similar to Cohen's d.
    """
    n1, n2 = len(group1), len(group2)
    try:
        u_stat, _ = mannwhitneyu(group1, group2, alternative='two-sided')
        r = 1 - (2*u_stat) / (n1 * n2)
        return r
    except:
        return np.nan


def format_pvalue(p):
    """Format p-value in scientific notation if very small"""
    if p < 0.001:
        return f"{p:.2e}"
    else:
        return f"{p:.4f}"


def perform_comprehensive_statistical_tests(algorithms, metric, output_dir='plots/comparison/statisticalData/'):
    """
    Perform comprehensive statistical comparison including:
    - t-test (parametric)
    - Wilcoxon (non-parametric, paired)
    - Mann-Whitney U (non-parametric, unpaired)
    - Effect sizes (Cohen's d, rank-biserial)
    - Win/loss counts
    """
    
    os.makedirs(output_dir, exist_ok=True)
    
    # Load data
    data = {}
    for algo, path in algorithms.items():
        df = pd.read_csv(path)
        data[algo] = df[metric].values
    
    # Generate all pairwise comparisons
    results = []
    pairs = itertools.combinations(algorithms.keys(), 2)
    
    for algo1, algo2 in pairs:
        values1 = data[algo1]
        values2 = data[algo2]
        
        # Basic statistics
        mean1, mean2 = np.mean(values1), np.mean(values2)
        std1, std2 = np.std(values1, ddof=1), np.std(values2, ddof=1)
        
        # Win/loss/tie counts (how many times algo1 beat algo2)
        wins = np.sum(values1 < values2)  # Lower is better for makespan/RT
        losses = np.sum(values1 > values2)
        ties = np.sum(values1 == values2)
        
        # T-test
        try:
            t_stat, t_pval = ttest_ind(values1, values2)
        except:
            t_stat, t_pval = np.nan, np.nan
        
        # Wilcoxon (paired non-parametric)
        try:
            w_stat, w_pval = wilcoxon(values1, values2)
        except:
            w_stat, w_pval = np.nan, np.nan
        
        # Mann-Whitney U (unpaired non-parametric)
        try:
            u_stat, u_pval = mannwhitneyu(values1, values2, alternative='two-sided')
        except:
            u_stat, u_pval = np.nan, np.nan
        
        # Effect sizes
        cohens_d = calculate_cohens_d(values1, values2)
        rank_bis = calculate_rank_biserial(values1, values2)
        
        # Statistical significance
        is_significant_t = t_pval < 0.05 if not np.isnan(t_pval) else False
        is_significant_w = w_pval < 0.05 if not np.isnan(w_pval) else False
        
        results.append({
            'Comparison': f'{algo1} vs {algo2}',
            'Mean_1': f'{mean1:.2f}',
            'Mean_2': f'{mean2:.2f}',
            'Std_1': f'{std1:.2f}',
            'Std_2': f'{std2:.2f}',
            'Diff': f'{mean1 - mean2:.2f}',
            'T-Test p-value': format_pvalue(t_pval) if not np.isnan(t_pval) else 'N/A',
            'Wilcoxon p-value': format_pvalue(w_pval) if not np.isnan(w_pval) else 'N/A',
            'Mann-Whitney p-value': format_pvalue(u_pval) if not np.isnan(u_pval) else 'N/A',
            "Cohen's d": f'{cohens_d:.3f}',
            'Effect Size': interpret_effect_size(cohens_d),
            'Rank-Biserial': f'{rank_bis:.3f}' if not np.isnan(rank_bis) else 'N/A',
            'Wins': wins,
            'Losses': losses,
            'Ties': ties,
            'Significant (p<0.05)': 'Yes' if (is_significant_t or is_significant_w) else 'No'
        })
    
    results_df = pd.DataFrame(results)
    
    # Save detailed results
    results_df.to_csv(f'{output_dir}/statistical_tests_detailed_{metric}.csv', index=False)
    
    # Create simplified table for paper
    paper_table = results_df[['Comparison', 'T-Test p-value', 'Wilcoxon p-value', 
                               "Cohen's d", 'Effect Size', 'Significant (p<0.05)']].copy()
    paper_table.to_csv(f'{output_dir}/statistical_tests_paper_{metric}.csv', index=False)
    
    print(f"\n{'='*70}")
    print(f"STATISTICAL TESTS - {metric}")
    print(f"{'='*70}\n")
    print(results_df.to_string(index=False))
    print(f"\n✓ Saved: statistical_tests_detailed_{metric}.csv")
    print(f"✓ Saved: statistical_tests_paper_{metric}.csv")
    
    return results_df


def calculate_algorithm_rankings(algorithms, metric, output_dir='data/comparison'):
    """
    Calculate overall rankings based on multiple criteria:
    - Mean performance
    - Median performance
    - Best case (min)
    - Worst case (max)
    - Consistency (std dev)
    """
    
    data = {}
    for algo, path in algorithms.items():
        df = pd.read_csv(path)
        data[algo] = df[metric].values
    
    rankings = []
    
    for algo in algorithms.keys():
        values = data[algo]
        
        rankings.append({
            'Algorithm': algo,
            'Mean': np.mean(values),
            'Median': np.median(values),
            'Std': np.std(values, ddof=1),
            'Min': np.min(values),
            'Max': np.max(values),
            'Range': np.max(values) - np.min(values),
            'CV (%)': (np.std(values, ddof=1) / np.mean(values)) * 100,
            'Q1': np.percentile(values, 25),
            'Q3': np.percentile(values, 75),
            'IQR': np.percentile(values, 75) - np.percentile(values, 25)
        })
    
    rankings_df = pd.DataFrame(rankings)
    
    # Sort by mean (lower is better for makespan/RT)
    rankings_df = rankings_df.sort_values('Mean')
    rankings_df['Rank'] = range(1, len(rankings_df) + 1)
    
    # Reorder columns
    cols = ['Rank', 'Algorithm', 'Mean', 'Median', 'Std', 'CV (%)', 'Min', 'Max', 'Range', 'IQR', 'Q1', 'Q3']
    rankings_df = rankings_df[cols]
    
    rankings_df.to_csv(f'{output_dir}/algorithm_rankings_{metric}.csv', index=False)
    
    print(f"\n{'='*70}")
    print(f"ALGORITHM RANKINGS - {metric}")
    print(f"{'='*70}\n")
    print(rankings_df.to_string(index=False))
    print(f"\n✓ Saved: algorithm_rankings_{metric}.csv")
    
    return rankings_df


# ============================================================================
# MAIN EXECUTION
# ============================================================================

if __name__ == "__main__":
    
    algorithms = {
        "RDA": "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/RDA_results.csv",
        "PSO": "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/PSO_results.csv",
        "WOA": "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/WOA_results.csv",
        "GA":  "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/GA_results.csv",
        "GWO": "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/GWO_results.csv"
    }
    
    # Run for each metric
    for metric in ['Makespan', 'AvgRT', 'AvgUtilization', 'TotalCost']:
        print(f"\n{'#'*70}")
        print(f"# PROCESSING: {metric}")
        print(f"{'#'*70}")
        
        # Comprehensive statistical tests
        perform_comprehensive_statistical_tests(algorithms, metric)
        
        # Algorithm rankings
        calculate_algorithm_rankings(algorithms, metric)
        
        print()