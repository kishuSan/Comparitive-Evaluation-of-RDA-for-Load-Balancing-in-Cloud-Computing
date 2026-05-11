import os
from plot_convergence_analysis import *
from enhanced_sttdev_boxplot import *

# Create output directory
os.makedirs('plots/convergence', exist_ok=True)

# =====================================
# SENSITIVITY PLOTS
# =====================================

csv_file = 'd:/ResearchPaper_LoadBalancer/data/sensitivity/Population_sensitivity.csv'

plot_parameter_sensitivity(
    csv_file,
    'Population',
    'Makespan'
)

plot_parameter_sensitivity(
    csv_file,
    'Population',
    'AvgRT'
)

plot_parameter_sensitivity(
    csv_file,
    'Population',
    'AvgUtilization'
)

# =====================================
# BOXPLOTS
# =====================================

plot_boxplot(
    csv_file,
    'Population',
    'Makespan'
)

plot_boxplot(
    csv_file,
    'Population',
    'AvgRT'
)

# =====================================
# STD DEV PLOTS
# =====================================

plot_stddev_analysis(
    csv_file,
    'Population',
    'Makespan'
)


# ===== POPULATION SENSITIVITY =====
csv_files = [
    'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Population_25.00.csv',
    'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Population_30.00.csv',
    'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Population_40.00.csv',
    'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Population_50.00.csv',
    'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Population_75.00.csv',
    'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Population_100.00.csv',
    'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Population_125.00.csv',
    'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Population_150.00.csv',
]
param_values = [25, 30, 40, 50, 75, 100, 125, 150]
param_name = 'Population'

# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Iterations_40.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Iterations_60.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Iterations_80.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Iterations_100.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Iterations_125.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Iterations_150.00.csv'
# ]
# param_values = [40, 60, 80, 100, 125, 150]
# param_name = 'Iterations'

# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.10.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.20.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.25.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.30.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.40.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.45.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.50.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.60.csv',
# ]
# param_values = [ 0.1, 0.2, 0.25, 0.3, 0.4, 0.45, 0.5, 0.6 ]
# param_name = 'Alpha'

# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.10.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.20.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.25.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.30.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.40.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.50.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Beta_0.60.csv',
# ]
# param_values = [ 0.1, 0.15, 0.2, 0.25, 0.3, 0.4, 0.5, 0.6 ]
# param_name = 'Beta'

# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.10.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.15.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.20.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.25.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.30.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.35.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.40.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.45.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.50.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Gamma_0.60.csv',
# ]
# param_values = [ 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6 ]
# param_name = 'Gamma'

# csv_files = [
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Cloudlets_100.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Cloudlets_250.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Cloudlets_500.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Cloudlets_750.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Cloudlets_1000.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Cloudlets_1250.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Cloudlets_1500.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Cloudlets_1750.00.csv',
#     'd:/ResearchPaper_LoadBalancer/data/convergence/convergence_Cloudlets_2000.00.csv'
# ]
# param_values = [ 100, 250, 500, 750, 1000, 1250, 1500, 1750, 2000 ]
# param_name = 'Cloudlets'

# # Generate individual convergence plots for each value
for csv_file, value in zip(csv_files, param_values):
    plot_convergence_curve_single_config(csv_file, value, param_name)
    plot_convergence_rate_analysis(csv_file, value, param_name)

# Generate comparison plot
plot_convergence_comparison_multiple_configs(csv_files, param_values, param_name)

# Generate summary statistics table
plot_convergence_statistics_table(csv_files, param_values, param_name)

# ===== REPEAT FOR OTHER PARAMETERS =====
# Alpha, Beta, Gamma, Iterations...