"""
Convergence Analysis Plots for RDA Algorithm
Addresses: Comment 6 (Convergence Analysis) and Comment 3 (Statistical Validation)
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from scipy import stats
from scipy.stats import t as t_dist
import os

# Set style for publication-quality plots
plt.style.use('seaborn-v0_8-paper')
sns.set_palette("husl")
plt.rcParams['figure.dpi'] = 300
plt.rcParams['savefig.dpi'] = 300
plt.rcParams['font.size'] = 10
plt.rcParams['axes.labelsize'] = 11
plt.rcParams['axes.titlesize'] = 12
plt.rcParams['xtick.labelsize'] = 9
plt.rcParams['ytick.labelsize'] = 9
plt.rcParams['legend.fontsize'] = 9

# Create output directory
os.makedirs('plots/convergence', exist_ok=True)


def calculate_confidence_interval(data, confidence=0.95):
    """
    Calculate confidence interval for given data
    
    Args:
        data: array-like data
        confidence: confidence level (default 0.95)
    
    Returns:
        mean, lower_bound, upper_bound, std_dev
    """
    n = len(data)
    mean = np.mean(data)
    std_dev = np.std(data, ddof=1)  # Sample standard deviation
    std_err = std_dev / np.sqrt(n)
    
    # t-distribution for confidence interval
    t_value = t_dist.ppf((1 + confidence) / 2, n - 1)
    margin_error = t_value * std_err
    
    return mean, mean - margin_error, mean + margin_error, std_dev


def plot_convergence_curve_single_config(csv_file, param_value, param_name, output_dir='plots/convergence'):
    """
    Plot convergence curve for a single configuration with confidence intervals.
    
    This shows how fitness improves over iterations.
    
    Args:
        csv_file: Path to CSV file with iteration-level fitness data
        param_value: Value of the parameter being tested
        param_name: Name of the parameter
        output_dir: Directory to save plots
    """
    # Read the CSV - expecting columns: Run, Iteration, Fitness
    df = pd.read_csv(csv_file)
    
    # Get unique iterations
    iterations = sorted(df['Iteration'].unique())
    num_iterations = len(iterations)
    
    # Calculate statistics for each iteration across all runs
    mean_fitness = []
    lower_ci = []
    upper_ci = []
    std_devs = []
    
    for iteration in iterations:
        iteration_data = df[df['Iteration'] == iteration]['Fitness'].values
        mean, lower, upper, std = calculate_confidence_interval(iteration_data)
        mean_fitness.append(mean)
        lower_ci.append(lower)
        upper_ci.append(upper)
        std_devs.append(std)
    
    # Create the plot
    fig, ax = plt.subplots(figsize=(8, 6))
    
    # Plot mean line
    ax.plot(iterations, mean_fitness, linewidth=2, label='Mean Fitness', color='navy')
    
    # Plot confidence interval as shaded region
    ax.fill_between(iterations, lower_ci, upper_ci, alpha=0.3, 
                     label='95% Confidence Interval', color='navy')
    
    # Plot standard deviation as error bars (every 10 iterations to avoid clutter)
    step = max(1, num_iterations // 10)
    ax.errorbar(iterations[::step], mean_fitness[::step], yerr=std_devs[::step], 
                fmt='o', markersize=4, capsize=4, capthick=1.5, alpha=0.7,
                label='±1 Std Dev', color='darkred')
    
    ax.set_xlabel('Iteration', fontweight='bold')
    ax.set_ylabel('Fitness Value', fontweight='bold')
    ax.set_title(f'Convergence Curve - {param_name} = {param_value}\n(Mean ± 95% CI across runs)', 
                 fontweight='bold')
    ax.legend(loc='best')
    ax.grid(True, alpha=0.3, linestyle='--')
    
    # Add text box with final statistics
    final_mean = mean_fitness[-1]
    final_std = std_devs[-1]
    final_ci_width = upper_ci[-1] - lower_ci[-1]
    
    textstr = f'Final Iteration:\n'
    textstr += f'Mean: {final_mean:.6f}\n'
    textstr += f'Std: {final_std:.6f}\n'
    textstr += f'95% CI width: {final_ci_width:.6f}'
    
    props = dict(boxstyle='round', facecolor='wheat', alpha=0.5)
    ax.text(0.02, 0.98, textstr, transform=ax.transAxes, fontsize=8,
            verticalalignment='top', bbox=props)
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/convergence_{param_name}_{param_value}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: convergence_{param_name}_{param_value}.png")


def plot_convergence_comparison_multiple_configs(csv_files, param_values, param_name, 
                                                   output_dir='plots/convergence'):
    """
    Compare convergence curves for multiple parameter configurations.
    
    Args:
        csv_files: List of CSV file paths
        param_values: List of parameter values corresponding to CSV files
        param_name: Name of the parameter being varied
        output_dir: Directory to save plots
    """
    fig, ax = plt.subplots(figsize=(10, 6))
    
    colors = plt.cm.viridis(np.linspace(0.1, 0.9, len(csv_files)))
    
    for idx, (csv_file, param_value) in enumerate(zip(csv_files, param_values)):
        df = pd.read_csv(csv_file)
        iterations = sorted(df['Iteration'].unique())
        
        mean_fitness = []
        lower_ci = []
        upper_ci = []
        
        for iteration in iterations:
            iteration_data = df[df['Iteration'] == iteration]['Fitness'].values
            mean, lower, upper, _ = calculate_confidence_interval(iteration_data)
            mean_fitness.append(mean)
            lower_ci.append(lower)
            upper_ci.append(upper)
        
        # Plot mean line
        label = f'{param_name}={param_value}'
        ax.plot(iterations, mean_fitness, linewidth=2, label=label, color=colors[idx])
        
        # Plot confidence interval (lighter shaded region)
        ax.fill_between(iterations, lower_ci, upper_ci, alpha=0.15, color=colors[idx])
    
    ax.set_xlabel('Iteration', fontweight='bold')
    ax.set_ylabel('Fitness Value', fontweight='bold')
    ax.set_title(f'Convergence Comparison - Effect of {param_name}\n(Mean ± 95% CI)', 
                 fontweight='bold')
    ax.legend(loc='best')
    ax.grid(True, alpha=0.3, linestyle='--')
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/convergence_comparison_{param_name}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: convergence_comparison_{param_name}.png")


def plot_convergence_rate_analysis(csv_file, param_value, param_name, 
                                    output_dir='plots/convergence'):
    """
    Analyze and plot convergence rate (improvement per iteration).
    
    This shows the derivative of fitness to understand convergence speed.
    
    Args:
        csv_file: Path to CSV file with iteration-level fitness data
        param_value: Value of the parameter being tested
        param_name: Name of the parameter
        output_dir: Directory to save plots
    """
    df = pd.read_csv(csv_file)
    iterations = sorted(df['Iteration'].unique())
    
    # Calculate mean fitness per iteration
    mean_fitness = []
    for iteration in iterations:
        iteration_data = df[df['Iteration'] == iteration]['Fitness'].values
        mean_fitness.append(np.mean(iteration_data))
    
    # Calculate improvement rate (negative derivative = improvement)
    improvement_rate = []
    for i in range(1, len(mean_fitness)):
        rate = mean_fitness[i-1] - mean_fitness[i]  # Positive = improvement
        improvement_rate.append(rate)
    
    # Create subplot figure
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(8, 8), sharex=True)
    
    # Top plot: Fitness curve
    ax1.plot(iterations, mean_fitness, linewidth=2, color='navy', marker='o', 
             markersize=3, markevery=max(1, len(iterations)//20))
    ax1.set_ylabel('Fitness Value', fontweight='bold')
    ax1.set_title(f'Convergence Analysis - {param_name} = {param_value}', fontweight='bold')
    ax1.grid(True, alpha=0.3, linestyle='--')
    
    # Bottom plot: Improvement rate
    ax2.bar(iterations[1:], improvement_rate, width=0.8, alpha=0.7, color='green', 
            label='Improvement (positive = better)')
    ax2.axhline(y=0, color='red', linestyle='--', linewidth=1, alpha=0.7)
    ax2.set_xlabel('Iteration', fontweight='bold')
    ax2.set_ylabel('Improvement Rate\n(Fitness decrease per iteration)', fontweight='bold')
    ax2.grid(True, alpha=0.3, linestyle='--', axis='y')
    ax2.legend()
    
    # Identify convergence point (when improvement < threshold for N consecutive iterations)
    threshold = 1e-6
    consecutive_needed = 5
    convergence_iteration = None
    
    consecutive_count = 0
    for i, rate in enumerate(improvement_rate):
        if abs(rate) < threshold:
            consecutive_count += 1
            if consecutive_count >= consecutive_needed:
                convergence_iteration = iterations[i - consecutive_needed + 2]
                break
        else:
            consecutive_count = 0
    
    if convergence_iteration:
        ax1.axvline(x=convergence_iteration, color='red', linestyle='--', linewidth=2, 
                    alpha=0.7, label=f'Convergence at iteration {convergence_iteration}')
        ax1.legend()
        ax2.axvline(x=convergence_iteration, color='red', linestyle='--', linewidth=2, alpha=0.7)
    
    plt.tight_layout()
    plt.savefig(f'{output_dir}/convergence_rate_{param_name}_{param_value}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: convergence_rate_{param_name}_{param_value}.png")
    
    return convergence_iteration


def plot_convergence_statistics_table(csv_files, param_values, param_name, 
                                       output_dir='plots/convergence'):
    """
    Create a statistical summary table for convergence analysis.
    
    Args:
        csv_files: List of CSV file paths
        param_values: List of parameter values
        param_name: Name of the parameter
        output_dir: Directory to save plots
    """
    summary_data = []
    
    for csv_file, param_value in zip(csv_files, param_values):
        df = pd.read_csv(csv_file)
        
        # Get final iteration data
        max_iteration = df['Iteration'].max()
        final_data = df[df['Iteration'] == max_iteration]['Fitness'].values
        
        # Get initial iteration data
        min_iteration = df['Iteration'].min()
        initial_data = df[df['Iteration'] == min_iteration]['Fitness'].values
        
        # Calculate statistics
        mean_final, ci_lower, ci_upper, std_final = calculate_confidence_interval(final_data)
        mean_initial = np.mean(initial_data)
        
        improvement = ((mean_initial - mean_final) / mean_initial) * 100
        
        summary_data.append({
            param_name: param_value,
            'Initial Fitness': f'{mean_initial:.6f}',
            'Final Fitness': f'{mean_final:.6f}',
            'Std Dev': f'{std_final:.6f}',
            '95% CI': f'[{ci_lower:.6f}, {ci_upper:.6f}]',
            'Improvement %': f'{improvement:.2f}%',
            'Iterations': max_iteration
        })
    
    summary_df = pd.DataFrame(summary_data)
    
    # Save to CSV
    summary_df.to_csv(f'{output_dir}/convergence_summary_{param_name}.csv', index=False)
    print(f"✓ Saved: convergence_summary_{param_name}.csv")
    
    # Create visual table
    fig, ax = plt.subplots(figsize=(12, len(param_values) * 0.6 + 1))
    ax.axis('tight')
    ax.axis('off')
    
    table = ax.table(cellText=summary_df.values, colLabels=summary_df.columns,
                     cellLoc='center', loc='center', 
                     colWidths=[0.12, 0.15, 0.15, 0.12, 0.25, 0.12, 0.09])
    
    table.auto_set_font_size(False)
    table.set_fontsize(9)
    table.scale(1, 2)
    
    # Style header
    for i in range(len(summary_df.columns)):
        table[(0, i)].set_facecolor('#4472C4')
        table[(0, i)].set_text_props(weight='bold', color='white')
    
    # Alternate row colors
    for i in range(1, len(summary_df) + 1):
        for j in range(len(summary_df.columns)):
            if i % 2 == 0:
                table[(i, j)].set_facecolor('#E7E6E6')
    
    plt.title(f'Convergence Statistics Summary - {param_name} Analysis\n', 
              fontweight='bold', fontsize=12, pad=20)
    plt.savefig(f'{output_dir}/convergence_table_{param_name}.png', bbox_inches='tight')
    plt.close()
    
    print(f"✓ Saved: convergence_table_{param_name}.png")
    
    return summary_df

def plot_stddev_analysis(
        csv_file,
        parameter_name,
        metric,
        output_dir='plots/robustness'
):
    os.makedirs(output_dir, exist_ok=True)

    df = pd.read_csv(csv_file)

    grouped = df.groupby("ParameterValue")

    x = []
    stds = []

    for param, group in grouped:

        values = group[metric].values

        x.append(param)
        stds.append(np.std(values))

    fig, ax = plt.subplots(figsize=(8,6))

    ax.plot(
        x,
        stds,
        marker='o',
        linewidth=2
    )

    ax.set_xlabel(parameter_name, fontweight='bold')
    ax.set_ylabel(
        f'StdDev of {metric}',
        fontweight='bold'
    )

    ax.set_title(
        f'Robustness Analysis: {parameter_name} vs StdDev({metric})',
        fontweight='bold'
    )

    ax.grid(True, alpha=0.3)

    plt.tight_layout()

    plt.savefig(
        f'{output_dir}/stddev_{parameter_name}_{metric}.png',
        bbox_inches='tight'
    )

    plt.close()

    print(
        f'✓ Saved: stddev_{parameter_name}_{metric}.png'
    )

def plot_boxplot(
        csv_file,
        parameter_name,
        metric,
        output_dir='plots/robustness'
):
    os.makedirs(output_dir, exist_ok=True)

    df = pd.read_csv(csv_file)

    fig, ax = plt.subplots(figsize=(10,6))

    sns.boxplot(
        x='ParameterValue',
        y=metric,
        data=df,
        ax=ax
    )

    ax.set_xlabel(parameter_name, fontweight='bold')
    ax.set_ylabel(metric, fontweight='bold')

    ax.set_title(
        f'Robustness Analysis: {metric} Distribution\nAcross {parameter_name}',
        fontweight='bold'
    )

    ax.grid(True, alpha=0.3)

    plt.tight_layout()

    plt.savefig(
        f'{output_dir}/boxplot_{parameter_name}_{metric}.png',
        bbox_inches='tight'
    )

    plt.close()

    print(
        f'✓ Saved: boxplot_{parameter_name}_{metric}.png'
    )


def plot_parameter_sensitivity(
        csv_file,
        parameter_name,
        metric,
        output_dir='plots/sensitivity'
):
    os.makedirs(output_dir, exist_ok=True)

    df = pd.read_csv(csv_file)

    grouped = df.groupby("ParameterValue")

    x = []
    means = []
    stds = []
    ci = []

    for param, group in grouped:

        values = group[metric].values

        mean, lower, upper, std = \
            calculate_confidence_interval(values)

        x.append(param)
        means.append(mean)
        stds.append(std)
        ci.append(upper - mean)

    fig, ax = plt.subplots(figsize=(8,6))

    ax.errorbar(
        x,
        means,
        yerr=ci,
        marker='o',
        capsize=5,
        linewidth=2
    )

    ax.set_xlabel(parameter_name, fontweight='bold')
    ax.set_ylabel(metric, fontweight='bold')

    ax.set_title(
        f'{parameter_name} vs {metric}\n(Mean ± 95% CI)',
        fontweight='bold'
    )

    ax.grid(True, alpha=0.3)

    plt.tight_layout()

    plt.savefig(
        f'{output_dir}/{parameter_name}_{metric}.png',
        bbox_inches='tight'
    )

    plt.close()

    print(
        f'✓ Saved: {parameter_name}_{metric}.png'
    )


# ============================================================================
# MAIN EXECUTION EXAMPLE
# ============================================================================

if __name__ == "__main__":
    print("=" * 60)
    print("CONVERGENCE ANALYSIS PLOT GENERATOR")
    print("=" * 60)
    print()
    
    # Example usage - you'll need to provide actual CSV files
    # CSV format expected: Iteration, Run, Fitness
    
    print("INSTRUCTIONS:")
    print("-" * 60)
    print("1. Prepare CSV files with columns: Iteration, Run, Fitness")
    print("2. Each row should contain fitness at a specific iteration for a specific run")
    print("3. Example CSV structure:")
    print()
    print("   Iteration,Run,Fitness")
    print("   0,0,0.856234")
    print("   1,0,0.782341")
    print("   2,0,0.723456")
    print("   ...")
    print("   0,1,0.843211")
    print("   1,1,0.776543")
    print("   ...")
    print()
    print("4. Run this script with your CSV files")
    print()
    print("EXAMPLE USAGE:")
    print("-" * 60)
    print("""
# For population sensitivity analysis:
csv_files = [
    'data/convergence_pop_10.csv',
    'data/convergence_pop_20.csv',
    'data/convergence_pop_30.csv',
    'data/convergence_pop_50.csv',
    'data/convergence_pop_100.csv'
]
param_values = [10, 20, 30, 50, 100]
param_name = 'Population'

# Generate individual convergence plots
for csv_file, value in zip(csv_files, param_values):
    plot_convergence_curve_single_config(csv_file, value, param_name)
    plot_convergence_rate_analysis(csv_file, value, param_name)

# Generate comparison plot
plot_convergence_comparison_multiple_configs(csv_files, param_values, param_name)

# Generate summary statistics
plot_convergence_statistics_table(csv_files, param_values, param_name)
    """)
    print()
    print("=" * 60)