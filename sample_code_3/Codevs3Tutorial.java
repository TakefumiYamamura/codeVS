import java.util.Scanner;

// codevs 3.0 Sample Program

public class Codevs3Tutorial {

	final int MAX_H = 20;
	final int MAX_W = 20;

	int turn;
	int MAX_TURN;
	int my_id; // 自分のプレイヤーID
	int H;
	int W;
	char[][] field = new char[MAX_H][MAX_W];

	int ch_size; // キャラクタの数
	int[] ch_pid; // キャラクタのプレイヤーID
	int[] ch_id; // キャラクタのID
	int[] ch_row; // 位置（行）
	int[] ch_col; // 位置（列）
	int[] ch_pow; // キャラクタの火力
	int[] ch_hav; // キャラクタが同時に出せる魔法陣数

	int magic_size; // フィールドにある魔法陣の個数
	int[] magic_cid; // 魔法陣のキャラクタID
	int[] magic_row; // 位置（行）
	int[] magic_col; // 位置（列）
	int[] magic_turn; // あと何ターンで発動するか
	int[] magic_pow; // 魔法陣の火力

	int item_size; // フィールドに出現しているアイテムの個数
	int[] item_type; // アイテムの種類 （0=NUMBER_UP, 1=POWER_UP）
	int[] item_row; // 位置（行）
	int[] item_col; // 位置（列）

	int[][] dist = new int[MAX_H][MAX_W];
	int[][] attack = new int[MAX_H][MAX_W];

	/**
	 * 盤面の状況を読み込む
	 */
	boolean input() {
		Scanner scanner = new Scanner(System.in);

		char[] str;

		turn = scanner.nextInt();
		MAX_TURN = scanner.nextInt();
		my_id = scanner.nextInt();
		H = scanner.nextInt();
		W = scanner.nextInt();
		for (int h = 0; h < H; h++) {
			str = scanner.next().toCharArray();
			for (int w = 0; w < W; w++) {
				// '@' は '.' に置き換えておく
				if (str[w] == '@')
					str[w] = '.';
				field[h][w] = str[w];
			}
		}

		ch_size = scanner.nextInt();
		ch_pid = new int[ch_size];
		ch_id = new int[ch_size];
		ch_row = new int[ch_size];
		ch_col = new int[ch_size];
		ch_pow = new int[ch_size];
		ch_hav = new int[ch_size];

		for (int i = 0; i < ch_size; i++) {
			ch_pid[i] = scanner.nextInt();
			ch_id[i] = scanner.nextInt();
			ch_row[i] = scanner.nextInt();
			ch_col[i] = scanner.nextInt();
			ch_pow[i] = scanner.nextInt();
			ch_hav[i] = scanner.nextInt();
		}

		magic_size = scanner.nextInt();
		magic_cid = new int[magic_size];
		magic_row = new int[magic_size];
		magic_col = new int[magic_size];
		magic_turn = new int[magic_size];
		magic_pow = new int[magic_size];
		for (int i = 0; i < magic_size; i++) {
			magic_cid[i] = scanner.nextInt();
			magic_row[i] = scanner.nextInt();
			magic_col[i] = scanner.nextInt();
			magic_turn[i] = scanner.nextInt();
			magic_pow[i] = scanner.nextInt();

			// 魔法陣があるマスも入れないので、'.'以外の文字を入れておく。
			field[magic_row[i]][magic_col[i]] = 'o';
		}

		item_size = scanner.nextInt();
		item_type = new int[item_size];
		item_row = new int[item_size];
		item_col = new int[item_size];
		for (int i = 0; i < item_size; i++) {
			str = scanner.next().toCharArray();
			item_type[i] = (str[0] == 'N' ? 0 : 1);
			item_row[i] = scanner.nextInt();
			item_col[i] = scanner.nextInt();
		}

		String endStr = scanner.next(); // 文字列「END」を読む

		if (endStr.equals("END")) {
			return true;
		} else {
			return false;
		}
	}

	int abs(int x) {
		return x < 0 ? -x : x;
	}

	int get_dist1(int x1, int y1, int x2, int y2) {
		return abs(x1 - x2) + abs(y1 - y2);
	}

	/**
	 * 全く攻撃してこない敵に、近づき、倒すことを目指し、 以下のアルゴリズムで行動する。
	 *
	 * 0. 簡単のため、自キャラ2体を同じマスに重ねて、以後同じ行動をさせる。
	 * 1. 移動できるマスのうち、敵に最も近いマスに移動する。
	 * 2. (1)で移動したマスに着いたら、魔法陣を設置する。
	 * 3. 魔法の発動に巻き込まれない場所まで移動する。
	 * 4. (1)に戻る。
	 */
	void solve() {
		int my0 = my_id * 2;
		int my1 = my_id * 2 + 1;
		int op0 = (1 - my_id) * 2;
		int op1 = (1 - my_id) * 2 + 1;

		// 現在の盤面をデバッグ出力
		// （クライアントで実行すると標準エラー出力は ./codevs30/log/io/ 配下にログが出力される）
		System.err.println("turn = " + turn);
		for (int i = 0; i < H; i++) {
			for (int j = 0; j < W; j++) {
				if (ch_row[my0] == i && ch_col[my0] == j) {
					System.err.print('M');
				} else {
					System.err.print(field[i][j]);
				}
			}
			System.err.println();
		}

		// (0) 自キャラ2体が違うマスにいる場合、片方を移動させて同じマスに移動させる。
		if (!(ch_row[my0] == ch_row[my1] && ch_col[my0] == ch_col[my1])) {
			String move = "NONE";
			if (ch_row[my0] > ch_row[my1]) { move = "DOWN"; }
			if (ch_row[my0] < ch_row[my1]) { move = "UP"; }
			if (ch_col[my0] > ch_col[my1]) { move = "RIGHT"; }
			if (ch_col[my0] < ch_col[my1]) { move = "LEFT"; }
			System.out.println("NONE"); // キャラクタ 0 の行動を出力
			System.out.println(move); // キャラクタ 1 の行動を出力
			return;
		}

		if (magic_size == 0) { // フィールドに魔法陣が一つもないとき、(1)、(2)の行動

			// 自キャラが、'.' を通っていける場所を列挙する。
			// dist[row][col] には、現在位置からの歩数が入る。
			calc_dist(ch_row[my0], ch_col[my0]);

			// 自キャラがいける場所のうちで、敵に最も近い場所を (target_row, target_col) に代入
			int target_row = ch_row[my0], target_col = ch_col[my0];
			for (int row = 0; row < H; row++)
				for (int col = 0; col < W; col++)
					if (dist[row][col] < 1000000)
						if (get_dist1(ch_row[op0], ch_col[op0], target_row, target_col) > get_dist1(ch_row[op0], ch_col[op0], row, col)) {
							target_row = row;
							target_col = col;
						}

			// (2) もし今いる場所が、自キャラがいける場所のうちで敵に最も近い場所ならば、魔法陣を設置。
			if (target_row == ch_row[my0] && target_col == ch_col[my0]) {
				System.out.println("NONE MAGIC 10");
				System.out.println("NONE");
				return;
			} else {
				// (1)
				// そうでないなら、目的地へ近づく向きへ一歩動く

				// 目的地からの距離を数えて、今いる場所から、近づく向きへ移動。
				String move = walk(target_row, target_col, ch_row[my0], ch_col[my0]);
				System.out.println(move);
				System.out.println(move);
				return;
			}

		} else {
			// (3) 魔法陣が1つでも置かれているなら、魔法に当たらない場所へ移動し、待機。

			// 魔法の攻撃範囲を求める。
			calc_attack();

			if (attack[ch_row[my0]][ch_col[my0]] == 0) { // 魔法に当たらない位置なら待機
				System.out.println("NONE");
				System.out.println("NONE");
				return;
			} else { // 今いる場所が魔法の攻撃範囲なら、よける。

				// 自キャラが、'.' を通っていける場所を列挙する。
				// dist[row][col] に、現在位置からの歩数が入る。
				calc_dist(ch_row[my0], ch_col[my0]);

				// 自キャラがいける場所のうちで、最も近い attack == 0 の場所を (target_row, target_col) に代入
				int target_row = -1, target_col = -1;
				for (int row = 0; row < H; row++)
					for (int col = 0; col < W; col++)
						if (dist[row][col] < 1000000 && attack[row][col] == 0)
							if (target_row == -1 || dist[target_row][target_col] > dist[row][col]) {
								target_row = row;
								target_col = col;
							}

				if (target_row == -1) {
					System.out.println("NONE");
					System.out.println("NONE");
					return;
				}

				// (target_row, target_col) に近づくように一歩移動
				// 目的地からの距離を数えて、今いる場所から、近づく向きへ移動。
				String move = walk(target_row, target_col, ch_row[my0], ch_col[my0]);
				System.out.println(move);
				System.out.println(move);
				return;
			}
		}
	}

	/**
	 * (start_row, start_col)からいける場所を列挙する。
	 * dist[row][co] には、 (start_row, start_col)からの歩数が格納される。いけない場所は大きい数が格納される。
	 */
	void calc_dist(int start_row, int start_col) {

		// distを大きい数で初期化
		for (int row = 0; row < H; row++) for (int col = 0; col < W; col++) dist[row][col] = 1000000;

		// 一歩ずつ隣を調べる。
		search(start_row, start_col, 0);
	}

	void search(int row, int col, int d) {
		// もう調べたマスは調べない。
		// 進入できるマス（何も無いマス or スタート地点）以外は調べない。
		if (dist[row][col] > d && (field[row][col] == '.' || d == 0)) {
			dist[row][col] = d;
			search(row + 1, col, d + 1);
			search(row - 1, col, d + 1);
			search(row, col + 1, d + 1);
			search(row, col - 1, d + 1);
		}
	}

	/**
	 * 魔法が発動したとき攻撃範囲となる場所を調べる。 attack に結果を格納。
	 *  attack[row][col] == 1 ならその場所は魔法の範囲内であることを表す。
	 *  attack[row][col] == 0 ならそれ以外
	 */
	void calc_attack() {
		for (int row = 0; row < H; row++) for (int col = 0; col < W; col++) attack[row][col] = 0;

		int[] dr = { +1, -1, 0, 0 };
		int[] dc = { 0, 0, +1, -1 };

		for (int i = 0; i < magic_size; i++) {
			for (int k = 0; k < 4; k++) {
				attack[magic_row[i]][magic_col[i]] = 1;
				int len = 1;
				int row = magic_row[i] + dr[k];
				int col = magic_col[i] + dc[k];
				while (field[row][col] == '.' && len <= magic_pow[i]) {
					attack[row][col] = 1;
					row += dr[k];
					col += dc[k];
					len++;
				}
			}

		}
	}

	/**
	 * 目的地からの距離を数えて、今いる場所から、近づく向きへ移動する方向を求める。
	 */
	String walk(int target_row, int target_col, int now_row, int now_col) {
		calc_dist(target_row, target_col);
		int row = now_row;
		int col = now_col;
		String move = "NONE";
		if (dist[row][col] > dist[row + 1][col]) { move = "DOWN"; }
		if (dist[row][col] > dist[row - 1][col]) { move = "UP"; }
		if (dist[row][col] > dist[row][col + 1]) { move = "RIGHT"; }
		if (dist[row][col] > dist[row][col - 1]) { move = "LEFT"; }
		return move;
	}



	public static void main(String[] args) {

		Codevs3Tutorial codevs3 = new Codevs3Tutorial();

		// AIの名前を出力
		System.out.println("tutorial_AI");
		System.out.flush();

		while (codevs3.input()) { // 正しい入力が受け取れる間ループする。入力が受け取れない場合 false を返すのでループを抜ける。
			codevs3.solve();
			System.out.flush();
		}
		return;
	}
}
