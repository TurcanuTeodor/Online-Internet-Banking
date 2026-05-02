package ro.app.fraud.tier3;

public final class MlUtils {

    private MlUtils() {
        // Private constructor to prevent instantiation
    }

    public static double[] computeMeans(double[][] data){
        double[] means = new double[data[0].length];
        for(double[] row : data){
            for(int j=0; j < row.length; j++){
                means[j] += row[j];
            }
        }
        for(int j=0; j < means.length; j++){
            means[j] /= data.length;
        }
        return means;
    }
    
    public static int argmax(double[] arr){
        int idx= 0;
        for(int i=0; i < arr.length; i++){
            if(arr[i] > arr[idx]){
                idx = i;
            }
        }
        return idx;
    }

    public static int argmax2(double[] arr){
        int first = argmax(arr), second= (first == 0) ? 1 : 0;
        for (int i=0; i< arr.length; i++){
            if(i != first && arr[i] > arr[second]){
                second = i;
            }
        }
        return second;
    }
}
