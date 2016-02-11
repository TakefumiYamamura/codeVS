import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

//
// codevs 4.0 Sample Program Java
//
public class SampleAI {

	// C++とJavaのサンプルコードで同じ動作をさせるために同じ擬似乱数を使う。
	int lcg = 0;
	int rand(int n) {
		lcg = 1664525 * lcg + 1013904223;
		int x = lcg % n;
		return x < 0 ? x + n : x;
	}

	class Unit {
		// ユニットの現在のステータス。
		// 与えられる入力により input() で設定される。
		int id;
		int y;
		int x;
		int hp;
		int type;

		// ユニットに出す命令。
		// 移動か生産のどちらかの行動を行うように think() で設定される（どちらの行動もとらない場合もある）。
		int movetoy = -1; // 非負の値ならそのマスへ向かって移動することを表す。
		int movetox = -1; // 非負の値ならそのマスへ向かって移動することを表す。
		int produce = -1; // 非負の値ならそのタイプのユニットを生産することを表す。
		boolean isMoving() {
			return movetoy >= 0;
		}
		boolean isProducing() {
			return produce >= 0;
		}
		boolean isFree() {
			return !isMoving() && !isProducing();
		}
		void free() {
			movetoy = -1;
			movetox = -1;
			produce = -1;
		}

		// 現在設定されている行動を出力用の文字列に変換する。
		// 行動が設定されてない場合は長さ 0 の文字列を返す。
		String toOrderString() {
			if (isProducing()) {
				return id + " " + produce;
			}
			if (isMoving()) {
				int dy = Math.abs(y - movetoy);
				int dx = Math.abs(x - movetox);
				if (dy + dx > 0) {
					if (rand(dy + dx) < dy) {
						if (y < movetoy) return id + " " + "D";
						if (y > movetoy) return id + " " + "U";
					} else {
						if (x < movetox) return id + " " + "R";
						if (x > movetox) return id + " " + "L";
					}
				}
				return "";
			}
			return "";
		}
	}

	int dist(int y1, int x1, int y2, int x2) {
		return Math.abs(y1 - y2) + Math.abs(x1 - x2);
	}

	static final int WORKER = 0;
	static final int KNIGHT = 1;
	static final int FIGHTER = 2;
	static final int ASSASSIN = 3;
	static final int CASTLE = 4;
	static final int WORKER_FACTORY = 5;
	static final int WARRIOR_FACTORY = 6;

	static final int VIEW_RANGE[] = {4, 4, 4, 4, 10, 10, 4};
	static final int BUILD_COST[] = {40, 20, 40, 60, 999999999, 100, 500};

	Scanner scanner = new Scanner(System.in);

	int remainingTime; // 残り持ち時間
	int currentResource; // 現在の資源数
	Unit myCastle = null; // 自分の城
	Unit opCastle = null; // 敵の城（敵の城を見つけていないならnull）
	boolean isTopLeft; // 1P側か2P側か
	Map<Integer, Unit> myUnits = new HashMap<Integer, Unit>(); // ユニットIDをキーとする自分のユニットの一覧。
	Map<Integer, Unit> opUnits = new HashMap<Integer, Unit>(); // ユニットIDをキーとする（視界内の）敵ユニットの一覧。
	boolean[][] resource = new boolean[100][100]; // そのマスに資源があるとtrue
	boolean[][] see = new boolean[100][100]; // そのマスが一度でも視界に入るとtrue

	void stageStart() {
		myUnits.clear();
		opUnits.clear();
		opCastle = null;
		for (int y = 0; y < 100; y++) for (int x = 0; x < 100; x++) resource[y][x] = false;
		for (int y = 0; y < 100; y++) for (int x = 0; x < 100; x++) see[y][x] = false;
	}

	boolean input() {
		remainingTime = scanner.nextInt();
		int currentStage = scanner.nextInt();
		int currentTurn = scanner.nextInt();
		if (currentTurn == 0) {
			// ターンをまたいで維持される変数はステージが新しくなった時点で初期化を行う。
			stageStart();
			// ステージが始まったことをデバッグ出力。
			// （クライアントで実行すると標準エラー出力は ./codevs40/log/io/ 配下にログが出力される）
			System.err.println("stage:" + currentStage);
		}

		currentResource = scanner.nextInt();

		{
			// 自分のユニットを読み込み。
			Map<Integer, Unit> mp = new HashMap<Integer, Unit>();
			int myNum = scanner.nextInt();
			for (int i = 0; i < myNum; i++) {
				int id = scanner.nextInt();
				// 前ターンから知っているユニットはそのまま使う。
				// （ユニットに設定した命令がターンをまたいでも引き継がれる実装）
				Unit u = myUnits.containsKey(id) ? myUnits.get(id) : new Unit();
				u.id = id;
				u.y = scanner.nextInt();
				u.x = scanner.nextInt();
				u.hp = scanner.nextInt();
				u.type = scanner.nextInt();
				mp.put(id, u);
				if (u.type == CASTLE) {
					myCastle = u;
					// 城が左上と右下のコーナーどちらに近いかで 1P側・2P側を判断
					isTopLeft = dist(myCastle.y, myCastle.x, 0, 0) <  dist(myCastle.y, myCastle.x, 99, 99);
				}
				// 一度でも視界に入ったことがあるマスを更新
				int view = VIEW_RANGE[u.type];
				for (int sy = u.y - view; sy <= u.y + view; sy++)
					for (int sx = u.x - view; sx <= u.x + view; sx++)
						if (0 <= sy && sy < 100 && 0 <= sx && sx < 100 && dist(u.y, u.x, sy, sx) <= view)
							see[sy][sx] = true;
			}
			// 死んだユニットをリストから除くため新しい map に追加して、置き換える。
			myUnits = mp;
		}

		{
			// 敵のユニットを読み込み
			Map<Integer, Unit> mp = new HashMap<Integer, Unit>();
			int opNum = scanner.nextInt();
			for (int i = 0; i < opNum; i++) {
				int id = scanner.nextInt();
				Unit u = opUnits.containsKey(id) ? opUnits.get(id) : new Unit();
				u.id = id;
				u.y = scanner.nextInt();
				u.x = scanner.nextInt();
				u.hp = scanner.nextInt();
				u.type = scanner.nextInt();
				mp.put(id, u);
				if (u.type == CASTLE) opCastle = u;
			}
			opUnits = mp;
		}

		{
			// 資源の位置を読み込み
			int resNum = scanner.nextInt();
			for (int i = 0; i < resNum; i++) {
				int y = scanner.nextInt();
				int x = scanner.nextInt();
				resource[y][x] = true;
			}
		}

		String end = scanner.next();
		if ("END".equals(end)) return true;
		return false;
	}

	// ワーカーの行動を考える
	void thinkWorker() {
		int[][] workerNum = new int[100][100]; // そのマスにワーカーが何人いるか。移動中のワーカーは目的地のマスでカウント。
		// 現状、マスごとにワーカーが何人いるか（向かっているか）を数える。
		for (Unit u : myUnits.values()) {
			if (u.type == WORKER) {
				if (u.isFree()) {
					workerNum[u.y][u.x]++;
				}
				if (u.isMoving()) {
					workerNum[u.movetoy][u.movetox]++;
				}
			}
		}

		// 命令を設定
		for (Unit u : myUnits.values()) {
			if (u.type == WORKER) {
				// 目的地についているなら命令を再設定
				if (u.isMoving() && u.y == u.movetoy && u.x == u.movetox) {
					u.free();
				}

				if (u.isFree()) {
					if (resource[u.y][u.x]) {
						// 資源上にいるなら何もしない
					} else {
						// 最も近い、獲得人数にあきがある資源へ向かう
						int ty = -1, tx = -1;
						for (int y = 0; y < 100; y++) for (int x = 0; x < 100; x++) {
							if (resource[y][x] && workerNum[y][x] < 5) {
								if (ty == -1 || dist(u.y, u.x, ty, tx) > dist(u.y, u.x, y, x)) {
									ty = y;
									tx = x;
								}
							}
						}
						if (ty == -1) {
							// 最も近い見えてないマスへ向かう
							for (int y = 0; y < 100; y++) for (int x = 0; x < 100; x++) {
								if (!see[y][x]) {
									if (ty == -1 || dist(u.y, u.x, ty, tx) > dist(u.y, u.x, y, x)) {
										ty = y;
										tx = x;
									}
								}
							}
						}
						if (ty != -1) {
							u.movetoy = ty;
							u.movetox = tx;
							workerNum[u.y][u.x]--;
							workerNum[u.movetoy][u.movetox]++;
						}
					}
				}
			}
		}
	}

	// 戦闘ユニットの行動を考える
	void thinkWarrior() {
		// 命令を設定
		for (Unit u : myUnits.values()) {
			if (u.type == KNIGHT || u.type == FIGHTER || u.type == ASSASSIN) {
				// 敵の城へ向かう
				int ty = -1, tx = -1;
				if (opCastle != null) {
					ty = opCastle.y;
					tx = opCastle.x;
				} else {
					// 敵の城の位置が不明なら敵の城がありそうな場所へ向かう
					int opCorner = isTopLeft ? 99 : 0;
					for (int y = 0; y < 100; y++) for (int x = 0; x < 100; x++) {
						if (!see[y][x]) {
							if (ty == -1 || dist(opCorner, opCorner, ty, tx) > dist(opCorner, opCorner, y, x)) {
								ty = y;
								tx = x;
							}
						}
					}
				}
				u.movetoy = ty;
				u.movetox = tx;
			}
		}
	}

	// 生産するユニットの行動を考える
	void thinkBuild() {
		int workerCount = 0; // 自分のワーカーの個数
		int warriorFactoryCount = 0; // 自分の拠点の個数
		// 現在のユニット数を数える
		for (Unit u : myUnits.values()) {
			if (u.type == WORKER) {
				workerCount++;
			}
			if (u.type == WARRIOR_FACTORY) {
				warriorFactoryCount++;
			}
		}

		// 命令を設定
		for (Unit u : myUnits.values()) {
			// ワーカーの数が一定数に満たないなら城でワーカーを作る
			if (u.type == CASTLE && workerCount < 100) {
				if (currentResource >= BUILD_COST[WORKER]) {
					currentResource -= BUILD_COST[WORKER];
					u.produce = WORKER;
				}
			}
			// 拠点が一つもないならワーカーで拠点を作る
			if (u.type == WORKER && warriorFactoryCount == 0 && u.isFree()) {
				if (currentResource >= BUILD_COST[WARRIOR_FACTORY]) {
					currentResource -= BUILD_COST[WARRIOR_FACTORY];
					u.produce = WARRIOR_FACTORY;
				}
			}
			// 拠点でナイト・ファイター・アサシンをランダムに作る
			if (u.type == WARRIOR_FACTORY) {
				int createdUnitType = KNIGHT + rand(3);
				if (currentResource >= BUILD_COST[createdUnitType]) {
					currentResource -= BUILD_COST[createdUnitType];
					u.produce = createdUnitType;
				}
			}
		}
	}

	void think() {
		// ユニットに命令を設定する
		thinkWorker();
		thinkWarrior();
		thinkBuild();

		// ユニットに設定された命令を文字列として出力する
		ArrayList<String> outputs = new ArrayList<String>();
		for (Unit u : myUnits.values()) {
			String s = u.toOrderString();
			if (s.length() > 0) {
				outputs.add(s);

				// このサンプルコードはユニットに設定した命令がターンをまたいでも引き継がれる実装をしているため、
				// 生産の命令は次ターンのために初期化しておく。
				if (u.isProducing()) u.free();
			}
		}

		System.out.println(outputs.size()); // 出力のはじめに命令の個数を出力
		for (String s : outputs)
			System.out.println(s); // 命令を一行ずつ出力
	}

	public static void main(String[] args) {
		SampleAI ai = new SampleAI();
		// AI の名前を出力
		System.out.println("SampleAI");
		System.out.flush(); // 忘れずに標準出力をフラッシュする
		while (ai.input()) { // 入力が読めない場合には false を返すのでループを抜ける
			ai.think();
			System.out.flush(); // 忘れずに標準出力をフラッシュする
		}
	}
}
