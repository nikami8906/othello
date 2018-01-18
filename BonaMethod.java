import java.util.*;

//ボナンザメソッドを実行するクラス
public class BonaMethod extends Evaluation {
    public double LearnRate = 0.01;
    public static double gain = 0.001;

    BonaMethod(String str) {
	super(str);
    }
    
    public void learnEvaluation ( ArrayList<Othello> othello ) {
	ArrayList<double[][]> v = new ArrayList<double[][]>();
	ArrayList<double[][]> vIndex = new ArrayList<double[][]>();
	ArrayList<double[][]> anser = new ArrayList<double[][]>();
	for (int i = 0; i < othello.size(); i++ ) {
	    v.add(getQ(othello.get(i)));
	    vIndex.add(getIndex(othello.get(i)));
	}
	//System.out.println(othello.size());
	int evalDiff = 0;
	
	for (int i = 0; i < v.size(); i++) {
	    for (int j = 0; j < v.get(0).length; j++) {
		for (int k = 0; k < v.get(0)[0].length; k++){
		    evalDiff += v.get(i)[j][k] - v.get(0)[j][k];
		}
	    }
	}

	for (int i2 = 0; i2 < v.size(); i2++) {
	    for (int j2 = 0; j2 < v.get(0).length; j2++) {
		for (int k2 = 0; k2 < v.get(0)[0].length; k2++){
		    
		    for (int i = 0; i < v.size(); i++) {
			double[][] vAnser = new double[8][12];	
			for (int j = 0; j < v.get(0).length; j++) {
			    for (int k = 0; k < v.get(0)[0].length; k++){
				if (vIndex.get(i2)[j2][k2] == vIndex.get(i)[j][k]) {
				    vAnser[j][k] += dSigmoid(evalDiff) * 1;
				}
				if (vIndex.get(i2)[j2][k2] == vIndex.get(0)[j][k]) {
				    vAnser[j][k] -= dSigmoid(evalDiff) * 1;
				}
			    }
			}
			anser.add(vAnser);
		    }
		}
	    }
	}
	for (int i = 0; i < v.size(); i++) {
	    //System.out.println("ok");
	    for (int j = 0; j < v.get(0).length; j++) {
		for (int k = 0; k < v.get(0)[0].length; k++){
		    v.get(i)[j][k] -= LearnRate * anser.get(i)[j][k];
		    //System.out.println(v.get(i)[j][k]);
		}
	    }
	}
	for (int i = 0; i < v.size(); i++) {
	    setEval(othello.get(i), v.get(i));
	}
	
    }
    

    public static double sigmoid(int x) {
	return 1/(1 + Math.exp(gain * x));
    }

    public static double dSigmoid(int x) {
	return (1 - sigmoid(x)) * sigmoid(x);
    }

}
