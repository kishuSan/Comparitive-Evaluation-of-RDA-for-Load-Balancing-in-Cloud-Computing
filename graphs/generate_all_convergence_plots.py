import os
from plot_convergence_analysis import *

# Create output directory
os.makedirs('plots/convergence', exist_ok=True)

# =====================================
# SENSITIVITY PLOTS
# =====================================

csv_file = 'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/sensitivity/Cloudlets_sensitivity.csv'

plot_parameter_sensitivity(
    csv_file,
    'Cloudlets',
    'Makespan'
)

plot_parameter_sensitivity(
    csv_file,
    'Cloudlets',
    'AvgRT'
)

plot_parameter_sensitivity(
    csv_file,
    'Cloudlets',
    'AvgUtilization'
)

# =====================================
# BOXPLOTS
# =====================================

plot_boxplot(
    csv_file,
    'Cloudlets',
    'Makespan'
)

plot_boxplot(
    csv_file,
    'Cloudlets',
    'AvgRT'
)

# =====================================
# STD DEV PLOTS
# =====================================

plot_stddev_analysis(
    csv_file,
    'Cloudlets',
    'Makespan'
)


# ===== POPULATION SENSITIVITY =====
# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Population_30.00.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Population_50.00.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Population_100.00.csv'
# ]
# param_values = [30, 50, 100]
# param_name = 'Population'

# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Iterations_20.00.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Iterations_50.00.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Iterations_100.00.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Iterations_150.00.csv'
# ]
# param_values = [20, 50, 100, 150]
# param_name = 'Iterations'

# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Alpha_0.10.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Alpha_0.20.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Alpha_0.30.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Alpha_0.40.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Alpha_0.50.csv',
# ]
# param_values = [0.10, 0.20, 0.30, 0.40, 0.50]
# param_name = 'Alpha'

# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Beta_0.10.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Beta_0.20.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Beta_0.30.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Beta_0.40.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Beta_0.50.csv',
# ]
# param_values = [0.10, 0.20, 0.30, 0.40, 0.50]
# param_name = 'Beta'

# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Gamma_0.20.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Gamma_0.30.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Gamma_0.40.csv',
#     'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Gamma_0.50.csv',
# ]
# param_values = [0.20, 0.30, 0.40, 0.50]
# param_name = 'Gamma'

csv_files = [
    'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Cloudlets_100.00.csv',
    'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Cloudlets_500.00.csv',
    'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Cloudlets_1000.00.csv',
    'd:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/convergence/convergence_Cloudlets_2000.00.csv'
]
param_values = [100, 500, 1000, 2000]
param_name = 'Cloudlets'

# Generate individual convergence plots for each value
for csv_file, value in zip(csv_files, param_values):
    plot_convergence_curve_single_config(csv_file, value, param_name)
    plot_convergence_rate_analysis(csv_file, value, param_name)

# Generate comparison plot
plot_convergence_comparison_multiple_configs(csv_files, param_values, param_name)

# Generate summary statistics table
plot_convergence_statistics_table(csv_files, param_values, param_name)

# ===== REPEAT FOR OTHER PARAMETERS =====
# Alpha, Beta, Gamma, Iterations...