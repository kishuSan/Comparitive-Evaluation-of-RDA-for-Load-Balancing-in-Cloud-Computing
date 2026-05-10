import pandas as pd
from scipy.stats import ttest_ind
from scipy.stats import wilcoxon
import itertools

# =========================================================
# LOAD DATA
# =========================================================

algorithms = {
    "RDA": "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/RDA_results.csv",
    "PSO": "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/PSO_results.csv",
    "WOA": "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/WOA_results.csv",
    "GA":  "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/GA_results.csv",
    "GWO": "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/GWO_results.csv"
}

metric = "Makespan"

data = {}

for algo, path in algorithms.items():

    df = pd.read_csv(path)

    data[algo] = df[metric].values

# =========================================================
# STATISTICAL TESTS
# =========================================================

results = []

pairs = itertools.combinations(
    algorithms.keys(),
    2
)

for algo1, algo2 in pairs:

    values1 = data[algo1]
    values2 = data[algo2]

    # ==========================================
    # T-TEST
    # ==========================================

    ttest = ttest_ind(
        values1,
        values2
    )

    # ==========================================
    # WILCOXON TEST
    # ==========================================

    try:
        wilcox = wilcoxon(
            values1,
            values2
        )

        wilcox_p = wilcox.pvalue

    except:
        wilcox_p = "ERROR"

    results.append({
        "Comparison":f"{algo1} vs {algo2}",
        "T-Test p-value":ttest.pvalue,
        "Wilcoxon p-value":wilcox_p
    })

# =========================================================
# RESULTS TABLE
# =========================================================

results_df = pd.DataFrame(results)

print("\n========== STATISTICAL TEST RESULTS ==========\n")

print(results_df)

# =========================================================
# SAVE CSV
# =========================================================

results_df.to_csv(
    "d:/ResearchPaper_LoadBalancer/code/load_balancer_sim_using_multiple_algos/data/comparison/statistical_tests.csv",
    index=False
)

print("\n✓ Saved: statistical_tests.csv")