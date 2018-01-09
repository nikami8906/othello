import java.util.*;

public class Evaluation {
    static int BLANK = 0;
    static int BLACK = 1;
    static int WHITE = 2;
    static int WALL = 3;
    static int END = 4;
    int ban[] = new int[100];
    int tPlayer, prePlayer;
    PatternEval[] pe = new PatternEval[61];
    int moveCount = 0;
    int ban8[][] = new int[8][100];
    int count = 0;
    double Lambda = 0.3;
    double DiscountFactor = 0.99;

    Evaluation() {
	init();
    }

    //パターンファイルを読み込む
    public void init() {
	for (int i = 0; i < 61; i++ ) {
	    pe[i] = PatternEval.readPatternFile(i);
	}
    }

    //盤面を格納するメソッド
    public void setGameData(Othello othello) {
	for (int i = 0; i < ban.length; i++) {
	    ban[i] = othello.ban[i];
	}
	tPlayer = othello.STATE;
	prePlayer = othello.prePlayer;
	moveCount = othello.moveCount;
    }

    //評価値を返す
    public int patternEvaluation(Othello othello) {
	setGameData(othello);
	if (prePlayer == WHITE) reverseAll();
	gen8ban();
	int score = 0;
	for (int i = 0; i<4; i++) {
	    score += diag4Eval(ban8[i]);
	    score += diag5Eval(ban8[i]);
	    score += diag6Eval(ban8[i]);
	    score += diag7Eval(ban8[i]);
	    score += diag8Eval(ban8[i]);
	    score += hor1Eval(ban8[i]);
	    score += hor2Eval(ban8[i]);
	    score += hor3Eval(ban8[i]);
	    score += hor4Eval(ban8[i]);
	    score += edgexEval(ban8[i]);
	    score += corner25Eval(ban8[i]);
	    score += corner33Eval(ban8[i]);
	}
	return score;
	
    }
    //評価関数のパラメータ更新
    //学習率0.1,割引率0.99,報酬は0
    //黒手番の評価を行う。必要があれば、白黒逆に
    
    public void learnEvaluation(Othello othello, Othello othello2) {
	learnEvaluation(othello, othello2, 1);
    }
    public void learnEvaluation(Othello othello, Othello othello2, double t_n) {


	
	double eligibility = DiscountFactor * Lambda;
	eligibility = Math.pow(eligibility, t_n - 1);
	if (eligibility < 0.0001) return ;
	
	double[][] q_t = new double[8][12];
	double[][] q_t1 = new double[8][12];
	double[][] valDiff = new double[8][12];

	q_t1 = getQ(othello2);
	q_t = getQ(othello);
	valDiff = valueDifference(q_t, q_t1);
	
	for (int i = 0; i < q_t.length; i++) {
	    for (int j = 0; j < q_t[1].length; j++) {
		q_t[i][j] = q_t[i][j] + 0.1 * eligibility * valDiff[i][j];
	    }
	}
	//System.out.println(Math.pow(e , t_n - 1));

	setEval(othello, q_t);

	//終端なら、ここで学習
	//次の相手の手番で終了するため、実質終端の場合は、
	//今の盤面を終端とし、最終的に得られる報酬を学習する。
	if (othello2.STATE == END) {
	    lastLearnEvaluation(othello2);
	} else {
	    Othello o = new Othello();
	    o.setBanInfo(othello2);
	    if (o.checkMove().size() == 1  && o.STATE != o.prePlayer ) {
		o.nextHand(o.checkMove().get(0));
		if (o.STATE == END) {
		    o.setBanInfo(othello2);
		    lastLearnEvaluation(o);
		}
	    }   
	}
    }
    //終端の評価値のパラメータを更新する
    //次状態を0,報酬を勝敗30000,石の差でx*1000とする
    public void lastLearnEvaluation(Othello othello) {
	double[][] q_t = new double[8][12];
	double[][] q_t1 = new double[8][12];
	double[][] valueDiff = new double[8][12];
	int reward = 0;

	
	for (int i = 0; i < q_t.length; i++) {
	    for (int j = 0; j < q_t[1].length; j++) {
		q_t1[i][j] = 0;
	    }
	}
	
	
	Othello o = new Othello();
	o.setBanInfo(othello);
	if (o.STATE != END ) {
	    o.nextHand(othello.checkMove().get(0));
	}
	setGameData(o);
	reward = (o.blackNum() - o.whiteNum()) * 1000;
	if (o.blackNum()- o.whiteNum()>0) {
	    reward += 30000;
	} else {
	    reward -= 30000;
	}
	if (othello.prePlayer == WHITE) reward *= -1;
	q_t = getQ(othello);
	valueDiff = valueDifference(q_t, q_t1, reward);

	
	for (int i = 0; i < q_t.length; i++) {
	    for (int j = 0; j < q_t[1].length; j++) {
		//System.out.println(valueDiff[i][j]);
		q_t[i][j] = q_t[i][j] + 0.1 * valueDiff[i][j];
	    }
	}

	setEval(othello, q_t);
    }

    //価値の誤差を返すメソッド
    public double[][] valueDifference (double[][] q_t, double[][] q_t1) {
	return valueDifference(q_t, q_t1, 0);
    }
    public double[][] valueDifference(double[][] q_t, double[][] q_t1, int reward) {
	double[][] valDiff = new double[8][12];
	for (int i = 0; i < q_t.length; i++) {
	    for (int j = 0; j < q_t[1].length; j++) {
		valDiff[i][j] = reward + DiscountFactor * q_t1[i][j] - q_t[i][j];
	    }
	}
	return valDiff;
    }
    
    public double[][] valueDifference(Othello othello, Othello othello2) {
	return valueDifference(othello, othello2, 0);
    }
    
    public double[][] valueDifference(Othello othello, Othello othello2, int reward ) {
	double[][] q_t = new double[8][12];
	double[][] q_t1 = new double[8][12];
	double[][] valDiff = new double[8][12];
	q_t1 = getQ(othello2);
	q_t = getQ(othello);
	for (int i = 0; i < q_t.length; i++) {
	    for (int j = 0; j < q_t[1].length; j++) {
		valDiff[i][j] = reward + DiscountFactor * q_t1[i][j] - q_t[i][j];
	    }
	}
	return valDiff;
    }
    

    public double[][] getQ (Othello othello) {
	double[][] q = new double[8][12];
	setGameData(othello);
	if (othello.prePlayer == othello.WHITE) reverseAll();
	gen8ban();
	
	for (int i = 0; i<8; i++) {
	    q[i][0] = diag4Eval(ban8[i]);
	    q[i][1] = diag5Eval(ban8[i]);
	    q[i][2] = diag6Eval(ban8[i]);
	    q[i][3] = diag7Eval(ban8[i]);
	    q[i][4] = diag8Eval(ban8[i]);
	    q[i][5] = hor1Eval(ban8[i]);
	    q[i][6] = hor2Eval(ban8[i]);
	    q[i][7] = hor3Eval(ban8[i]);
	    q[i][8] = hor4Eval(ban8[i]);
	    q[i][9] = edgexEval(ban8[i]);
	    q[i][10] = corner25Eval(ban8[i]);
	    q[i][11] = corner33Eval(ban8[i]);
	}
	return q;
    }
    

    public void setEval(Othello othello, double[][] q_t) {
	setGameData(othello);
	if (othello.prePlayer == othello.WHITE) reverseAll();
	for (int i = 0; i<8; i++) {
	    setDiag4Eval(ban8[i], (int)q_t[i][0]);
	    setDiag5Eval(ban8[i], (int)q_t[i][1]);
	    setDiag6Eval(ban8[i], (int)q_t[i][2]);
	    setDiag7Eval(ban8[i], (int)q_t[i][3]);
	    setDiag8Eval(ban8[i], (int)q_t[i][4]);
	    setHor1Eval(ban8[i], (int)q_t[i][5]);
	    setHor2Eval(ban8[i], (int)q_t[i][6]);
	    setHor3Eval(ban8[i], (int)q_t[i][7]);
	    setHor4Eval(ban8[i], (int)q_t[i][8]);
	    setEdgexEval(ban8[i], (int)q_t[i][9]);
	    setCorner25Eval(ban8[i], (int)q_t[i][10]);
	    setCorner33Eval(ban8[i], (int)q_t[i][11]);
	}
    }

    //盤面の駒の色を逆にする
    public void reverseAll() {
	for (int i = 0; i < ban.length; i++) {
	    if (ban[i] == BLACK) {
		ban[i] = WHITE;
	    }else if (ban[i] == WHITE) {
		ban[i] = BLACK;
	    }
	}
    }

    //現在と等価な盤面を8個生み出す
    public void gen8ban () {
	for (int i = 0; i < ban.length; i++) {
	    int x = i/10;
	    int y = i%10;
	    ban8[0][i] = ban[i];
	    ban8[1][y*10+(9-x)] = ban[i];
	    ban8[2][(9-x)*10+(9-y)] = ban[i];
	    ban8[3][(9-y)*10+x] = ban[i];
	}
	for (int i=0; i<ban.length; i++) {
	    ban8[4][99-i] = ban8[0][i];
	    ban8[5][99-i] = ban8[1][i];
	    ban8[6][99-i] = ban8[2][i];
	    ban8[7][99-i] = ban8[3][i];
	}
    }

    //評価値を格納する
    public void setDiag4Eval(int[] b, int eval) {
	int pattern = b[14];
	pattern = 3 * pattern + b[23];
	pattern = 3 * pattern + b[32];
	pattern = 3 * pattern + b[41];
	pe[moveCount].diag4[pattern] = eval;
    }
    public void setDiag5Eval(int[] b, int eval) {
	int pattern = b[15];
	pattern = 3 * pattern + b[24];
	pattern = 3 * pattern + b[33];
	pattern = 3 * pattern + b[42];
	pattern = 3 * pattern + b[51];
	pe[moveCount].diag5[pattern] = eval;
    }
    public void setDiag6Eval(int[] b, int eval) {
	int pattern = b[16];
	pattern = 3 * pattern + b[25];
	pattern = 3 * pattern + b[34];
	pattern = 3 * pattern + b[43];
	pattern = 3 * pattern + b[52];
	pattern = 3 * pattern + b[61];
	pe[moveCount].diag6[pattern] = eval;
    }
    public void setDiag7Eval(int[] b, int eval) {
	int pattern = b[17];
	pattern = 3 * pattern + b[26];
	pattern = 3 * pattern + b[35];
	pattern = 3 * pattern + b[44];
	pattern = 3 * pattern + b[53];
	pattern = 3 * pattern + b[62];
	pattern = 3 * pattern + b[71];
	pe[moveCount].diag7[pattern] = eval;
    }
    public void setDiag8Eval(int[] b, int eval) {
	int pattern = b[18];
	pattern = 3 * pattern + b[27];
	pattern = 3 * pattern + b[36];
	pattern = 3 * pattern + b[45];
	pattern = 3 * pattern + b[54];
	pattern = 3 * pattern + b[63];
	pattern = 3 * pattern + b[72];
	pattern = 3 * pattern + b[81];
	pe[moveCount].diag8[pattern] = eval;
    }
    public void setHor1Eval(int[] b, int eval) {
	int pattern = b[11];
	pattern = 3 * pattern + b[12];
	pattern = 3 * pattern + b[13];
	pattern = 3 * pattern + b[14];
	pattern = 3 * pattern + b[15];
	pattern = 3 * pattern + b[16];
	pattern = 3 * pattern + b[17];
	pattern = 3 * pattern + b[18];
	pe[moveCount].hor1[pattern] = eval;
    }
    public void setHor2Eval(int[] b, int eval) {
	int pattern = b[21];
	pattern = 3 * pattern + b[22];
	pattern = 3 * pattern + b[23];
	pattern = 3 * pattern + b[24];
	pattern = 3 * pattern + b[25];
	pattern = 3 * pattern + b[26];
	pattern = 3 * pattern + b[27];
	pattern = 3 * pattern + b[28];
	pe[moveCount].hor2[pattern] = eval;
    }
    public void setHor3Eval(int[] b, int eval) {
	int pattern = b[11];
	pattern = 3 * pattern + b[32];
	pattern = 3 * pattern + b[33];
	pattern = 3 * pattern + b[34];
	pattern = 3 * pattern + b[35];
	pattern = 3 * pattern + b[36];
	pattern = 3 * pattern + b[37];
	pattern = 3 * pattern + b[38];
	pe[moveCount].hor3[pattern] = eval;
    }
    public void setHor4Eval(int[] b, int eval) {
	int pattern = b[41];
	pattern = 3 * pattern + b[42];
	pattern = 3 * pattern + b[43];
	pattern = 3 * pattern + b[44];
	pattern = 3 * pattern + b[45];
	pattern = 3 * pattern + b[46];
	pattern = 3 * pattern + b[47];
	pattern = 3 * pattern + b[48];
	pe[moveCount].hor4[pattern] = eval;
    }
    public void setEdgexEval(int[] b, int eval) {
	int pattern = b[11];
	pattern = 3 * pattern + b[12];
	pattern = 3 * pattern + b[13];
	pattern = 3 * pattern + b[14];
	pattern = 3 * pattern + b[15];
	pattern = 3 * pattern + b[16];
	pattern = 3 * pattern + b[17];
	pattern = 3 * pattern + b[18];
	pattern = 3 * pattern + b[22];
	pattern = 3 * pattern + b[27];
	pe[moveCount].edgex[pattern] = eval;
	
    }
    
    public void setCorner25Eval(int[] b, int eval) {
	int pattern = b[11];
	pattern = 3 * pattern + b[12];
	pattern = 3 * pattern + b[13];
	pattern = 3 * pattern + b[14];
	pattern = 3 * pattern + b[15];
	pattern = 3 * pattern + b[21];
	pattern = 3 * pattern + b[22];
	pattern = 3 * pattern + b[23];
	pattern = 3 * pattern + b[24];
	pattern = 3 * pattern + b[25];
	pe[moveCount].corner25[pattern] = eval;
	
    }
    public void setCorner33Eval(int[] b, int eval) {
	int pattern = b[11];
	pattern = 3 * pattern + b[12];
	pattern = 3 * pattern + b[13];
	pattern = 3 * pattern + b[21];
	pattern = 3 * pattern + b[22];
	pattern = 3 * pattern + b[23];
	pattern = 3 * pattern + b[31];
	pattern = 3 * pattern + b[32];
	pattern = 3 * pattern + b[33];
	pe[moveCount].corner33[pattern] = eval;
	
    }

    //評価値を取得する
    public int diag4Eval(int[] b) {
	int pattern = b[14];
	pattern = 3 * pattern + b[23];
	pattern = 3 * pattern + b[32];
	pattern = 3 * pattern + b[41];
	return pe[moveCount].diag4[pattern];
    }
    public int diag5Eval(int[] b) {
	int pattern = b[15];
	pattern = 3 * pattern + b[24];
	pattern = 3 * pattern + b[33];
	pattern = 3 * pattern + b[42];
	pattern = 3 * pattern + b[51];
	return pe[moveCount].diag5[pattern];
    }
    public int diag6Eval(int[] b) {
	int pattern = b[16];
	pattern = 3 * pattern + b[25];
	pattern = 3 * pattern + b[34];
	pattern = 3 * pattern + b[43];
	pattern = 3 * pattern + b[52];
	pattern = 3 * pattern + b[61];
	return pe[moveCount].diag6[pattern];
    }
    public int diag7Eval(int[] b) {
	int pattern = b[17];
	pattern = 3 * pattern + b[26];
	pattern = 3 * pattern + b[35];
	pattern = 3 * pattern + b[44];
	pattern = 3 * pattern + b[53];
	pattern = 3 * pattern + b[62];
	pattern = 3 * pattern + b[71];
	return pe[moveCount].diag7[pattern];
    }
    public int diag8Eval(int[] b) {
	int pattern = b[18];
	pattern = 3 * pattern + b[27];
	pattern = 3 * pattern + b[36];
	pattern = 3 * pattern + b[45];
	pattern = 3 * pattern + b[54];
	pattern = 3 * pattern + b[63];
	pattern = 3 * pattern + b[72];
	pattern = 3 * pattern + b[81];
	return pe[moveCount].diag8[pattern];
    }
    public int hor1Eval(int[] b) {
	int pattern = b[11];
	pattern = 3 * pattern + b[12];
	pattern = 3 * pattern + b[13];
	pattern = 3 * pattern + b[14];
	pattern = 3 * pattern + b[15];
	pattern = 3 * pattern + b[16];
	pattern = 3 * pattern + b[17];
	pattern = 3 * pattern + b[18];
	return pe[moveCount].hor1[pattern];
    }
    public int hor2Eval(int[] b) {
	int pattern = b[21];
	pattern = 3 * pattern + b[22];
	pattern = 3 * pattern + b[23];
	pattern = 3 * pattern + b[24];
	pattern = 3 * pattern + b[25];
	pattern = 3 * pattern + b[26];
	pattern = 3 * pattern + b[27];
	pattern = 3 * pattern + b[28];
	return pe[moveCount].hor2[pattern];
    }
    public int hor3Eval(int[] b) {
	int pattern = b[11];
	pattern = 3 * pattern + b[32];
	pattern = 3 * pattern + b[33];
	pattern = 3 * pattern + b[34];
	pattern = 3 * pattern + b[35];
	pattern = 3 * pattern + b[36];
	pattern = 3 * pattern + b[37];
	pattern = 3 * pattern + b[38];
	return pe[moveCount].hor3[pattern];
    }
    public int hor4Eval(int[] b) {
	int pattern = b[41];
	pattern = 3 * pattern + b[42];
	pattern = 3 * pattern + b[43];
	pattern = 3 * pattern + b[44];
	pattern = 3 * pattern + b[45];
	pattern = 3 * pattern + b[46];
	pattern = 3 * pattern + b[47];
	pattern = 3 * pattern + b[48];
	return pe[moveCount].hor4[pattern];
    }
    public int edgexEval(int[] b) {
	int pattern = b[11];
	pattern = 3 * pattern + b[12];
	pattern = 3 * pattern + b[13];
	pattern = 3 * pattern + b[14];
	pattern = 3 * pattern + b[15];
	pattern = 3 * pattern + b[16];
	pattern = 3 * pattern + b[17];
	pattern = 3 * pattern + b[18];
	pattern = 3 * pattern + b[22];
	pattern = 3 * pattern + b[27];
	return pe[moveCount].edgex[pattern];
    }
    public int corner25Eval(int[] b) {
	int pattern = b[11];
	pattern = 3 * pattern + b[12];
	pattern = 3 * pattern + b[13];
	pattern = 3 * pattern + b[14];
	pattern = 3 * pattern + b[15];
	pattern = 3 * pattern + b[21];
	pattern = 3 * pattern + b[22];
	pattern = 3 * pattern + b[23];
	pattern = 3 * pattern + b[24];
	pattern = 3 * pattern + b[25];
	return pe[moveCount].corner25[pattern];
    }
    public int corner33Eval(int[] b) {
	int pattern = b[11];
	pattern = 3 * pattern + b[12];
	pattern = 3 * pattern + b[13];
	pattern = 3 * pattern + b[21];
	pattern = 3 * pattern + b[22];
	pattern = 3 * pattern + b[23];
	pattern = 3 * pattern + b[31];
	pattern = 3 * pattern + b[32];
	pattern = 3 * pattern + b[33];
	return pe[moveCount].corner33[pattern];
    }
}