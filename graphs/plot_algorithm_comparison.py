"""
Algorithm Comparison Plots with Statistical Rigor
Addresses: Comment 3 (Statistical Validation) and Comment 9 (Analytical Interpretation)
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from scipy.stats import t as t_dist
import os

plt.style.use('seaborn-v0_8-paper')
sns.set_palette("Set2")
plt.rcParams['figure.dpi'] = 300
plt.rcParams['font.size'] = 10


def calculate_ci(data, confidence=0.95):
    """Calculate mean and confidence interval"""
    n = len(data)
    mean = np.mean(data)
    std = np.std(data, ddof=1)
    se = std / np.sqrt(n)
    t_val = t_dist.ppf((1 + confidence) / 2, n - 1)
    margin = t_val * se
    return mean, mean - margin, mean + margin, std


def plot_algorithm_comparison_bars(algorithms, metric, output_dir='plots/comparison'):
    """
    Bar chart comparing algorithms with error bars showing 95% CI.
    """
    os.makedirs(output_dir, exist_ok=True)
    
    data = {}
    for algo, path in algorithms.items():
        df = pd.read_csv(path)
        data[algo] = df[metric].values
    
    # Calculate statistics
    stats = []
    for algo in algorithms.keys():
        values = data[algo]
        mean, ci_lower, ci_upper, std = calculate_ci(values)
        
        stats.append({
            'Algorithm': algo,
            'Mean': mean,
            'CI_Lower': ci_lower,
            'CI_Upper': ci_upper,
            'CI_Width': ci_upper - ci_lower,
            'Std': std
        })
    
    stats_df = pd.DataFrame(stats).sort_values('Mean')
    
    # Create plot
    fig, ax = plt.subplots(figsize=(10, 6))
    
    x = np.arange(len(stats_df))
    width = 0.6
    
    # Plot bars with CI error bars
    bars = ax.bar(x, stats_df['Mean'], width, alpha=0.8,
                   yerr=[stats_df['Mean'] - stats_df['CI_Lower'],
                         stats_df['CI_Upper'] - stats_df['Mean']],
                   capsize=8, error_kw={'linewidth': 2, 'elinewidth': 1.5})
    
    # Color bars by performance (gradient)
    norm = plt.Normalize(stats_df['Mean'].min(), stats_df['Mean'].max())
    colors = plt.cm.RdYlGn_r(norm(stats_df['Mean']))
    for bar, color in zip(bars, colors):
        bar.set_color(color)
    
    # Add value labels
    for i, (mean, ci_width) in enumerate(zip(stats_df['Mean'], stats_df['CI_Width'])):
        ax.text(i, mean, f'{mean:.2f}\n±{ci_width/2:.2f}',
                ha='center', va='bottom', fontsize=9, fontweight='bold')
    
    ax.set_xticks(x)
    ax.set_xticklabels(stats_df['Algorithm'], fontsize=11, fontweight='bold')
    ax.set_ylabel(f'{metric}', fontweight='bold', fontsize=12)
    ax.set_title(f'Algorithm Comparison: {metric}\n(Mean ± 95% Confidence Interval)',
                 fontweight='bold', fontsize=13)
    ax.grid(True, alpha=0.3, axis='y')
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/comparison_bars_{metric}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: comparison_bars_{metric}.png")
    return stats_df


def plot_algorithm_comparison_violin(algorithms, metric, output_dir='plots/comparison'):
    """
    Violin plot showing full distribution of algorithm performance.
    Combines boxplot and density estimation.
    """
    os.makedirs(output_dir, exist_ok=True)
    
    # Load all data into single dataframe
    all_data = []
    for algo, path in algorithms.items():
        df = pd.read_csv(path)
        df['Algorithm'] = algo
        all_data.append(df[[metric, 'Algorithm']])
    
    combined_df = pd.concat(all_data, ignore_index=True)
    
    # Create plot
    fig, ax = plt.subplots(figsize=(12, 7))
    
    # Violin plot with inner box
    parts = ax.violinplot(
        [combined_df[combined_df['Algorithm'] == algo][metric].values 
         for algo in algorithms.keys()],
        positions=range(len(algorithms)),
        showmeans=True,
        showmedians=True,
        widths=0.7
    )
    
    # Color violins
    colors = sns.color_palette("Set2", len(algorithms))
    for i, pc in enumerate(parts['bodies']):
        pc.set_facecolor(colors[i])
        pc.set_alpha(0.7)
        pc.set_edgecolor('black')
        pc.set_linewidth(1.5)
    
    # Style mean/median lines
    parts['cmedians'].set_edgecolor('red')
    parts['cmedians'].set_linewidth(2)
    parts['cmeans'].set_edgecolor('blue')
    parts['cmeans'].set_linewidth(2)
    
    # Overlay box plot for quartiles
    bp = ax.boxplot(
        [combined_df[combined_df['Algorithm'] == algo][metric].values 
         for algo in algorithms.keys()],
        positions=range(len(algorithms)),
        widths=0.3,
        patch_artist=False,
        showfliers=False,
        boxprops=dict(linewidth=1.5, color='black'),
        whiskerprops=dict(linewidth=1.5, color='black'),
        capprops=dict(linewidth=1.5, color='black'),
        medianprops=dict(linewidth=0)  # Hide median (already shown in violin)
    )
    
    ax.set_xticks(range(len(algorithms)))
    ax.set_xticklabels(list(algorithms.keys()), fontsize=11, fontweight='bold')
    ax.set_ylabel(f'{metric}', fontweight='bold', fontsize=12)
    ax.set_title(f'Algorithm Distribution Comparison: {metric}\n(Violin + Box Plot with Quartiles)',
                 fontweight='bold', fontsize=13)
    ax.grid(True, alpha=0.3, axis='y')
    
    # Add legend
    from matplotlib.lines import Line2D
    legend_elements = [
        Line2D([0], [0], color='red', lw=2, label='Median'),
        Line2D([0], [0], color='blue', lw=2, label='Mean'),
        Line2D([0], [0], color='black', lw=1.5, label='IQR (Q1-Q3)')
    ]
    ax.legend(handles=legend_elements, loc='best')
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/comparison_violin_{metric}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: comparison_violin_{metric}.png")


def plot_algorithm_performance_radar(algorithms, metrics, output_dir='plots/comparison'):
    """
    Radar chart comparing algorithms across multiple metrics.
    Shows multidimensional performance profile.
    """
    os.makedirs(output_dir, exist_ok=True)
    
    # Collect normalized performance scores
    perf_data = {algo: [] for algo in algorithms.keys()}
    
    for metric in metrics:
        # Load data
        data = {}
        for algo, path in algorithms.items():
            df = pd.read_csv(path)
            data[algo] = np.mean(df[metric].values)
        
        # Normalize to 0-1 (invert for metrics where lower is better)
        values = list(data.values())
        min_val, max_val = min(values), max(values)
        
        # For makespan, RT, cost: lower is better → invert
        # For utilization: higher is better → don't invert
        invert = metric not in ['AvgUtilization']
        
        for algo in algorithms.keys():
            if max_val != min_val:
                if invert:
                    # Invert: best (min) → 1.0, worst (max) → 0.0
                    score = 1 - ((data[algo] - min_val) / (max_val - min_val))
                else:
                    # Normal: best (max) → 1.0, worst (min) → 0.0
                    score = (data[algo] - min_val) / (max_val - min_val)
            else:
                score = 1.0
            
            perf_data[algo].append(score)
    
    # Create radar chart
    angles = np.linspace(0, 2 * np.pi, len(metrics), endpoint=False).tolist()
    angles += angles[:1]  # Complete the circle
    
    fig, ax = plt.subplots(figsize=(10, 10), subplot_kw=dict(projection='polar'))
    
    colors = sns.color_palette("Set2", len(algorithms))
    
    for i, (algo, scores) in enumerate(perf_data.items()):
        scores_plot = scores + scores[:1]  # Complete the circle
        ax.plot(angles, scores_plot, 'o-', linewidth=2, label=algo, color=colors[i])
        ax.fill(angles, scores_plot, alpha=0.15, color=colors[i])
    
    ax.set_xticks(angles[:-1])
    ax.set_xticklabels(metrics, fontsize=10, fontweight='bold')
    ax.set_ylim(0, 1)
    ax.set_yticks([0.2, 0.4, 0.6, 0.8, 1.0])
    ax.set_yticklabels(['0.2', '0.4', '0.6', '0.8', '1.0'], fontsize=8)
    ax.grid(True)
    
    plt.title('Multi-Metric Performance Radar\n(Normalized Scores: 1.0 = Best, 0.0 = Worst)',
              fontweight='bold', fontsize=13, pad=20)
    plt.legend(loc='upper right', bbox_to_anchor=(1.3, 1.1), fontsize=10)
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/performance_radar.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: performance_radar.png")


def plot_pairwise_improvement_matrix(algorithms, metric, output_dir='plots/comparison'):
    """
    Heatmap showing percentage improvement of each algorithm over others.
    Helps identify which algorithm is superior and by how much.
    """
    os.makedirs(output_dir, exist_ok=True)
    
    # Load mean performance
    means = {}
    for algo, path in algorithms.items():
        df = pd.read_csv(path)
        means[algo] = np.mean(df[metric].values)
    
    algo_list = list(algorithms.keys())
    improvement_matrix = np.zeros((len(algo_list), len(algo_list)))
    
    # Calculate improvement percentages
    for i, algo1 in enumerate(algo_list):
        for j, algo2 in enumerate(algo_list):
            if i != j:
                # Percentage improvement = (other - self) / other * 100
                # Positive = algo1 is better than algo2
                improvement = ((means[algo2] - means[algo1]) / means[algo2]) * 100
                improvement_matrix[i, j] = improvement
    
    # Create heatmap
    fig, ax = plt.subplots(figsize=(10, 8))
    
    im = sns.heatmap(improvement_matrix, annot=True, fmt='.2f',
                     xticklabels=algo_list, yticklabels=algo_list,
                     cmap='RdYlGn', center=0, cbar_kws={'label': '% Improvement'},
                     linewidths=1, linecolor='white', ax=ax)
    
    ax.set_title(f'Pairwise Improvement Matrix: {metric}\n(Row improves over Column by X%)',
                 fontweight='bold', fontsize=13, pad=15)
    ax.set_xlabel('Compared To (Column)', fontweight='bold', fontsize=11)
    ax.set_ylabel('Algorithm (Row)', fontweight='bold', fontsize=11)
    
    # Rotate labels
    plt.setp(ax.get_xticklabels(), rotation=45, ha='right')
    plt.setp(ax.get_yticklabels(), rotation=0)
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/improvement_matrix_{metric}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: improvement_matrix_{metric}.png")


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
    
    metrics = ['Makespan', 'AvgRT', 'AvgUtilization', 'TotalCost']
    
    print("=" * 70)
    print("ALGORITHM COMPARISON PLOT GENERATOR")
    print("=" * 70)
    print()
    
    # Generate comparison plots for each metric
    for metric in metrics:
        print(f"\nProcessing: {metric}")
        print("-" * 50)
        
        plot_algorithm_comparison_bars(algorithms, metric)
        plot_algorithm_comparison_violin(algorithms, metric)
        plot_pairwise_improvement_matrix(algorithms, metric)
    
    # Multi-metric radar chart
    plot_algorithm_performance_radar(algorithms, metrics)
    
    print("\n" + "=" * 70)
    print("COMPLETE!")
    print("=" * 70)