"""
Robustness Analysis Plots
Addresses: Comment 7 (Robustness Analysis)

Generates:
1. Coefficient of Variation (CV) comparison
2. Interquartile Range (IQR) analysis
3. Min-Max range visualization
4. Stability score heatmap
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import os

# Plot settings
plt.style.use('seaborn-v0_8-paper')
sns.set_palette("husl")
plt.rcParams['figure.dpi'] = 300
plt.rcParams['savefig.dpi'] = 300
plt.rcParams['font.size'] = 10


def plot_coefficient_variation_comparison(algorithms, metric, output_dir='plots/robustness'):
    """
    Plot Coefficient of Variation (CV) - measures relative variability.
    Lower CV = more robust/consistent algorithm.
    
    CV = (std / mean) * 100
    """
    os.makedirs(output_dir, exist_ok=True)
    
    data = {}
    for algo, path in algorithms.items():
        df = pd.read_csv(path)
        data[algo] = df[metric].values
    
    # Calculate CV for each algorithm
    cv_data = []
    for algo, values in data.items():
        mean_val = np.mean(values)
        std_val = np.std(values, ddof=1)
        cv = (std_val / mean_val) * 100 if mean_val != 0 else 0
        
        cv_data.append({
            'Algorithm': algo,
            'CV (%)': cv,
            'Mean': mean_val,
            'Std': std_val
        })
    
    cv_df = pd.DataFrame(cv_data).sort_values('CV (%)')
    
    # Create plot
    fig, ax = plt.subplots(figsize=(8, 6))
    
    bars = ax.barh(cv_df['Algorithm'], cv_df['CV (%)'], 
                    color=plt.cm.RdYlGn_r(cv_df['CV (%)'] / cv_df['CV (%)'].max()))
    
    # Add value labels
    for i, bar in enumerate(bars):
        width = bar.get_width()
        ax.text(width + 0.1, bar.get_y() + bar.get_height()/2,
                f'{width:.2f}%',
                ha='left', va='center', fontsize=9)
    
    ax.set_xlabel('Coefficient of Variation (%)', fontweight='bold')
    ax.set_ylabel('Algorithm', fontweight='bold')
    ax.set_title(f'Robustness Comparison: Coefficient of Variation\n{metric} (Lower = More Robust)',
                 fontweight='bold')
    ax.grid(True, alpha=0.3, axis='x')
    
    # Add interpretation zone
    ax.axvline(x=5, color='green', linestyle='--', alpha=0.5, linewidth=1, label='CV < 5%: Excellent')
    ax.axvline(x=10, color='orange', linestyle='--', alpha=0.5, linewidth=1, label='CV < 10%: Good')
    ax.legend(loc='lower right', fontsize=8)
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/cv_comparison_{metric}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: cv_comparison_{metric}.png")
    return cv_df


def plot_iqr_comparison(algorithms, metric, output_dir='plots/robustness'):
    """
    Plot Interquartile Range (IQR) comparison.
    IQR = Q3 - Q1 (middle 50% spread)
    Lower IQR = more concentrated results = better consistency
    """
    os.makedirs(output_dir, exist_ok=True)
    
    data = {}
    for algo, path in algorithms.items():
        df = pd.read_csv(path)
        data[algo] = df[metric].values
    
    # Calculate IQR statistics
    iqr_data = []
    for algo, values in data.items():
        q1 = np.percentile(values, 25)
        q3 = np.percentile(values, 75)
        iqr = q3 - q1
        median = np.median(values)
        
        iqr_data.append({
            'Algorithm': algo,
            'IQR': iqr,
            'Q1': q1,
            'Q3': q3,
            'Median': median
        })
    
    iqr_df = pd.DataFrame(iqr_data).sort_values('IQR')
    
    # Create plot
    fig, ax = plt.subplots(figsize=(10, 6))
    
    x = np.arange(len(iqr_df))
    width = 0.6
    
    # Plot IQR as bars
    bars = ax.bar(x, iqr_df['IQR'], width, alpha=0.7, label='IQR (Q3-Q1)')
    
    # Overlay Q1 and Q3 as error markers
    ax.scatter(x, iqr_df['Q1'], color='green', s=100, marker='_', linewidths=3, 
               label='Q1 (25th percentile)', zorder=3)
    ax.scatter(x, iqr_df['Q3'], color='red', s=100, marker='_', linewidths=3,
               label='Q3 (75th percentile)', zorder=3)
    ax.scatter(x, iqr_df['Median'], color='blue', s=60, marker='o',
               label='Median', zorder=4)
    
    # Connect Q1-Q3 with vertical lines
    for i in range(len(iqr_df)):
        ax.plot([i, i], [iqr_df.iloc[i]['Q1'], iqr_df.iloc[i]['Q3']],
                color='black', linewidth=2, alpha=0.5)
    
    ax.set_xticks(x)
    ax.set_xticklabels(iqr_df['Algorithm'], rotation=0)
    ax.set_xlabel('Algorithm', fontweight='bold')
    ax.set_ylabel(f'{metric} Value', fontweight='bold')
    ax.set_title(f'Robustness: Interquartile Range Analysis\n{metric} (Narrower IQR = Better Consistency)',
                 fontweight='bold')
    ax.legend(loc='best')
    ax.grid(True, alpha=0.3, axis='y')
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/iqr_comparison_{metric}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: iqr_comparison_{metric}.png")
    return iqr_df


def plot_min_max_range(algorithms, metric, output_dir='plots/robustness'):
    """
    Plot Min-Max range showing best and worst case performance.
    Narrower range = more predictable/robust.
    """
    os.makedirs(output_dir, exist_ok=True)
    
    data = {}
    for algo, path in algorithms.items():
        df = pd.read_csv(path)
        data[algo] = df[metric].values
    
    # Calculate statistics
    stats_data = []
    for algo, values in data.items():
        stats_data.append({
            'Algorithm': algo,
            'Min': np.min(values),
            'Max': np.max(values),
            'Mean': np.mean(values),
            'Range': np.max(values) - np.min(values)
        })
    
    stats_df = pd.DataFrame(stats_data).sort_values('Mean')
    
    # Create plot
    fig, ax = plt.subplots(figsize=(10, 6))
    
    x = np.arange(len(stats_df))
    
    # Plot range as error bars from min to max
    for i, row in stats_df.iterrows():
        ax.plot([i, i], [row['Min'], row['Max']], 
                color='gray', linewidth=8, alpha=0.3, solid_capstyle='round')
    
    # Plot mean as marker
    ax.scatter(x, stats_df['Mean'], s=150, color='navy', marker='D',
               label='Mean', zorder=3, edgecolors='white', linewidth=1.5)
    
    # Plot min/max
    ax.scatter(x, stats_df['Min'], s=80, color='green', marker='v',
               label='Best (Min)', zorder=2)
    ax.scatter(x, stats_df['Max'], s=80, color='red', marker='^',
               label='Worst (Max)', zorder=2)
    
    ax.set_xticks(x)
    ax.set_xticklabels(stats_df['Algorithm'], rotation=0)
    ax.set_xlabel('Algorithm', fontweight='bold')
    ax.set_ylabel(f'{metric} Value', fontweight='bold')
    ax.set_title(f'Robustness: Min-Max Range Analysis\n{metric} (Narrower Range = More Predictable)',
                 fontweight='bold')
    ax.legend(loc='best')
    ax.grid(True, alpha=0.3, axis='y')
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/minmax_range_{metric}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: minmax_range_{metric}.png")
    return stats_df


def plot_robustness_heatmap(algorithms, metrics, output_dir='plots/robustness'):
    """
    Create heatmap showing robustness scores across multiple metrics.
    
    Robustness score = 1 / CV (normalized)
    Higher score = more robust
    """
    os.makedirs(output_dir, exist_ok=True)
    
    robustness_matrix = []
    algo_names = []
    
    for algo, path in algorithms.items():
        algo_names.append(algo)
        df = pd.read_csv(path)
        
        row_scores = []
        for metric in metrics:
            if metric in df.columns:
                values = df[metric].values
                mean_val = np.mean(values)
                std_val = np.std(values, ddof=1)
                cv = (std_val / mean_val) * 100 if mean_val != 0 else 100
                
                # Robustness score (inverse of CV, normalized)
                robustness_score = 1 / (1 + cv/10)  # Scale to 0-1
                row_scores.append(robustness_score)
            else:
                row_scores.append(0)
        
        robustness_matrix.append(row_scores)
    
    robustness_df = pd.DataFrame(robustness_matrix, 
                                  index=algo_names, 
                                  columns=metrics)
    
    # Create heatmap
    fig, ax = plt.subplots(figsize=(10, 6))
    
    sns.heatmap(robustness_df, annot=True, fmt='.3f', cmap='RdYlGn',
                cbar_kws={'label': 'Robustness Score'},
                linewidths=1, linecolor='white',
                vmin=0, vmax=1, ax=ax)
    
    ax.set_title('Robustness Score Heatmap Across Metrics\n(Higher = More Robust/Consistent)',
                 fontweight='bold', pad=20)
    ax.set_xlabel('Metric', fontweight='bold')
    ax.set_ylabel('Algorithm', fontweight='bold')
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/robustness_heatmap.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: robustness_heatmap.png")
    
    # Save numerical data
    robustness_df.to_csv(f'{output_dir}/robustness_scores.csv')
    print(f"✓ Saved: robustness_scores.csv")
    
    return robustness_df


def plot_stability_over_parameters(csv_file, parameter_name, metric, 
                                     output_dir='plots/robustness'):
    """
    Show how algorithm stability (CV) changes as parameter varies.
    Tests robustness to parameter changes.
    """
    os.makedirs(output_dir, exist_ok=True)
    
    df = pd.read_csv(csv_file)
    
    grouped = df.groupby('ParameterValue')
    
    param_values = []
    cv_values = []
    mean_values = []
    std_values = []
    
    for param_val, group in grouped:
        values = group[metric].values
        mean_val = np.mean(values)
        std_val = np.std(values, ddof=1)
        cv = (std_val / mean_val) * 100 if mean_val != 0 else 0
        
        param_values.append(param_val)
        cv_values.append(cv)
        mean_values.append(mean_val)
        std_values.append(std_val)
    
    # Create dual-axis plot
    fig, ax1 = plt.subplots(figsize=(10, 6))
    
    color1 = 'tab:blue'
    ax1.set_xlabel(f'{parameter_name}', fontweight='bold')
    ax1.set_ylabel(f'{metric} (Mean ± Std)', fontweight='bold', color=color1)
    ax1.errorbar(param_values, mean_values, yerr=std_values,
                 fmt='o-', linewidth=2, capsize=5, color=color1,
                 label=f'{metric} Mean ± Std')
    ax1.tick_params(axis='y', labelcolor=color1)
    ax1.grid(True, alpha=0.3)
    
    # Second y-axis for CV
    ax2 = ax1.twinx()
    color2 = 'tab:red'
    ax2.set_ylabel('Coefficient of Variation (%)', fontweight='bold', color=color2)
    ax2.plot(param_values, cv_values, 's--', linewidth=2, color=color2,
             markersize=8, label='CV (%)')
    ax2.tick_params(axis='y', labelcolor=color2)
    
    # Title
    plt.title(f'Stability Analysis: {parameter_name} Sensitivity\n{metric} Performance and Robustness',
              fontweight='bold', pad=20)
    
    # Combine legends
    lines1, labels1 = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    ax1.legend(lines1 + lines2, labels1 + labels2, loc='best')
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/stability_{parameter_name}_{metric}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: stability_{parameter_name}_{metric}.png")


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
    print("ROBUSTNESS ANALYSIS PLOT GENERATOR")
    print("=" * 70)
    print()
    
    # Generate robustness plots for each metric
    for metric in metrics:
        print(f"\nProcessing: {metric}")
        print("-" * 50)
        
        plot_coefficient_variation_comparison(algorithms, metric)
        plot_iqr_comparison(algorithms, metric)
        plot_min_max_range(algorithms, metric)
    
    # Multi-metric heatmap
    plot_robustness_heatmap(algorithms, metrics)
    
    # Parameter stability analysis (example for RDA)
    print("\nParameter Stability Analysis:")
    print("-" * 50)
    
    # Example: Population sensitivity
    plot_stability_over_parameters(
        'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/sensitivity/Population_sensitivity.csv',
        'Population',
        'Makespan'
    )
    
    print("\n" + "=" * 70)
    print("COMPLETE!")
    print("=" * 70)