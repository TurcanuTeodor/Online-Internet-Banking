package ro.app.fraud.tier3;

import smile.anomaly.IsolationForest;

public final class PerturbationAnalyzer {
    
    private PerturbationAnalyzer() {}

    public static double[] computeFeatureImportances(double[] features, IsolationForest model, double[] featureMeans){
        
        double baseScore = model.score(features);
        double[] importances = new double[features.length];

        for(int i=0; i< features.length; i++){
            double[] perturbed = features.clone();
            perturbed[i] =featureMeans[i]; //neutralize feature by replacing with mean
            importances[i] = Math.abs(baseScore - model.score(perturbed)); //importance = how much score changes when feature is neutralized
        }
        return importances;
    }
}
