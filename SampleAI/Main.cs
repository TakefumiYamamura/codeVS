using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace codevs5sample
{
    class Program
    {
        void Run()
        {
            AI ai = new AI();
            Console.WriteLine(ai.GetName()); // はじめにAI名を出力
            Console.Out.Flush(); // 忘れずにフラッシュする

            for (;;)
            {
                try {
                    ai.ReadInput();
                } catch (Exception e) {
                    Console.Error.WriteLine(e.StackTrace);
                    break;
                }
                Output output = ai.Think();
                Console.Write(output.ToOutputString());  // 改行を2回出力しないように注意すること
                Console.Out.Flush();  // 忘れずにフラッシュする
            }
        }

        static void Main(string[] args)
        {
            new Program().Run();
        }
    }

    internal class AI
    {
        public string GetName() {
            return "SampleAI.cs";
        }

        const char MAP_EMPTY = '_';
        const char MAP_OBSTACLE = 'O';
        const char MAP_WALL = 'W';

        Random random = new Random();

        int remainedTimeMills;
        int[] skillCosts;
        Board[] boards;

        public void ReadInput()
        {
            remainedTimeMills = int.Parse(ReadLine()); // 残り時間
            skillCosts = new int[int.Parse(ReadLine())]; // スキルの個数
            string[] skillCostsStr = ReadLine().Split(); // スキルの使用コスト
            for (int i = 0; i < skillCosts.Length; i++) skillCosts[i] = int.Parse(skillCostsStr[i]);
            boards = new Board[2];
            for (int playerId = 0; playerId < 2; playerId++)
            {
                Board board = new Board();

                board.skillPoint = int.Parse(ReadLine()); // 所持ポイント
                string[] HWStr = ReadLine().Split(); // マップの縦横の長さ
                board.H = int.Parse(HWStr[0]);
                board.W = int.Parse(HWStr[1]);
                board.map = new char[board.H * board.W]; // マップ情報
                for (int r = 0; r < board.H; r++)
                {
                    string line = ReadLine();
                    for (int c = 0; c < board.W; c++)
                    {
                        board.map[r * board.W + c] = line[c];
                    }
                }

                board.ninjas = new int[int.Parse(ReadLine())]; // プレイヤーキャラ情報
                for (int i = 0; i < board.ninjas.Length; i++)
                {
                    string[] ninjaStr = ReadLine().Split();
                    int id = int.Parse(ninjaStr[0]);
                    int r = int.Parse(ninjaStr[1]);
                    int c = int.Parse(ninjaStr[2]);
                    board.ninjas[i] = r * board.W + c;
                }

                board.dogs = new int[int.Parse(ReadLine())]; // 犬の情報
                for (int i = 0; i < board.dogs.Length; i++)
                {
                    string[] dogStr = ReadLine().Split();
                    int id = int.Parse(dogStr[0]);
                    int r = int.Parse(dogStr[1]);
                    int c = int.Parse(dogStr[2]);
                    board.dogs[i] = r * board.W + c;
                }

                board.items = new int[int.Parse(ReadLine())]; // アイテムの情報
                for (int i = 0; i < board.items.Length; i++)
                {
                    string[] itemStr = ReadLine().Split();
                    int r = int.Parse(itemStr[0]);
                    int c = int.Parse(itemStr[1]);
                    board.items[i] = r * board.W + c;
                }

                board.skillUsedCount = new int[skillCosts.Length]; // スキルの使用回数
                string[] skillUsedCountStr = ReadLine().Split();
                for (int i = 0; i < board.skillUsedCount.Length; i++)
                {
                    board.skillUsedCount[i] = int.Parse(skillUsedCountStr[i]);
                }

                boards[playerId] = board;
            }
        }


        private string ReadLine()
        {
            for (;;)
            {
                string line = Console.ReadLine();
                if (line == null) throw new Exception("EOF");
                if (line.Trim().Length == 0) continue;
                return line;
            }
        }

        public Output Think()
        {
            Output output = new Output();

            Board board = boards[0];
            Board enemyBoard = boards[1];

            int HW = board.H * board.W;
            const int INF = 1000000;

            // ランダムでスキルを使う
            Skill skill = (Skill)random.Next(skillCosts.Length);
            if (board.skillPoint >= skillCosts[(int)skill]) {
                switch (skill) {
                    case Skill.SPEED_UP:
                        output.skill = ((int)skill).ToString();
                        break;
                    case Skill.PUT_OBJECT:
                        for (int retry = 0; retry < 50; retry++) // 自分のマップのランダムな空マスを選ぶ
                        {  
                            int pos = random.Next(HW);
                            if (board.map[pos] == MAP_EMPTY) {
                                output.skill = ((int)skill).ToString() + " " + (pos / board.W) + " " + (pos % board.W);
                                break;
                            }
                        }
                        break;
                    case Skill.PUT_OBJECT_ENEMY:
                        for (int retry = 0; retry < 50; retry++)  // 相手のマップのランダムな空マスを選ぶ
                        {
                            int pos = random.Next(HW);
                            if (enemyBoard.map[pos] == MAP_EMPTY)
                            {
                                output.skill = ((int)skill).ToString() + " " + (pos / board.W) + " " + (pos % board.W);
                                break;
                            }
                        }
                        break;
                    case Skill.DELETE_OBJECT:
                        for (int retry = 0; retry < 50; retry++)  // 自分のマップのランダムな障害物マスを選ぶ
                        {
                            int pos = random.Next(HW);
                            if (board.map[pos] == MAP_OBSTACLE)
                            {
                                output.skill = ((int)skill).ToString() + " " + (pos / board.W) + " " + (pos % board.W);
                                break;
                            }
                        }
                        break;
                    case Skill.DELETE_OBJECT_ENEMY:
                        for (int retry = 0; retry < 50; retry++)  // 相手のマップのランダムな障害物マスを選ぶ
                        {
                            int pos = random.Next(HW);
                            if (enemyBoard.map[pos] == MAP_OBSTACLE)
                            {
                                output.skill = ((int)skill).ToString() + " " + (pos / board.W) + " " + (pos % board.W);
                                break;
                            }
                        }
                        break;
                    case Skill.PUT_DECOY:
                        for (int retry = 0; retry < 50; retry++)  // 自分のマップのランダムな空マスを選ぶ
                        {
                            int pos = random.Next(HW);
                            if (board.map[pos] == MAP_EMPTY)
                            {
                                output.skill = ((int)skill).ToString() + " " + (pos / board.W) + " " + (pos % board.W);
                                break;
                            }
                        }
                        break;
                    case Skill.PUT_DECOY_ENEMY:
                        for (int retry = 0; retry < 50; retry++)  // 相手のマップのランダムな空マスを選ぶ
                        {
                            int pos = random.Next(HW);
                            if (enemyBoard.map[pos] == MAP_EMPTY)
                            {
                                output.skill = ((int)skill).ToString() + " " + (pos / board.W) + " " + (pos % board.W);
                                break;
                            }
                        }
                        break;
                    case Skill.HOLY_IMMOLATION:
                        int id = random.Next(2); // どちらのキャラで発動させるかをランダムで選ぶ
                        output.skill = ((int)skill).ToString() + " " + id;
                        break;
                    default:
                        break;
                }
            }

            // キャラの移動を計算する
            
            int[] dpos = { -board.W, -1, +1, board.W };
            string[] dstr = { "U", "L", "R", "D" };

            bool[] dogMap = new bool[board.map.Length]; // 犬がいるマスとその隣のマスがtrue
            foreach (int dogPos in board.dogs) {
                dogMap[dogPos] = true;
                for (int d = 0; d < 4; d++) dogMap[dogPos + dpos[d]] = true;
            }

            for (int id = 0; id < board.ninjas.Length; id++)
            {
                // 幅優先探索で最も近くのアイテムへ向かう
                int[] dist = new int[HW]; // 今のマスからの歩数
                for (int i = 0; i < dist.Length; i++) dist[i] = INF;
                int[] trace = new int[HW]; // 一つ前のマスからの移動方向
                Queue<int> q = new Queue<int>();
                int startPos = board.ninjas[id];
                q.Enqueue(startPos);
                dist[startPos] = 0;

                int lastPos = -1;
                while (q.Count > 0)
                {
                    int pos = q.Dequeue();
                    if (Array.IndexOf(board.items, pos) != -1) // アイテムのマスに到達したら終わる
                    {
                        board.items[Array.IndexOf(board.items, pos)] = -1; // 2キャラが同じアイテムへ向かわないように消す
                        lastPos = pos;
                        break;
                    }
                    for (int d = 0; d < 4; d++)
                    {
                        int nextPos = pos + dpos[d];
                        // 障害物・犬・犬の隣マスは進入不可のマスとして扱う
                        if (board.map[nextPos] == MAP_EMPTY && !dogMap[nextPos] && dist[nextPos] == INF)
                        {
                            dist[nextPos] = dist[pos] + 1;
                            trace[nextPos] = d;
                            q.Enqueue(nextPos);
                        }
                    }
                }

                if (lastPos != -1)
                {
                    // 経路復元
                    List<int> back = new List<int>();
                    for (; lastPos != startPos;)
                    {
                        back.Add(trace[lastPos]);
                        lastPos -= dpos[trace[lastPos]];
                    }
                    if (back.Count >= 1) output.moves[id] += dstr[back[back.Count - 1]];
                    if (back.Count >= 2) output.moves[id] += dstr[back[back.Count - 2]];
                    // 3マス歩くスキルを使っているならもう一歩動く
                    if (output.skill == "0" && back.Count >= 3) output.moves[id] += dstr[back[back.Count - 3]];
                }
            }

            return output;
        }
    }

    internal class Board
    {
        public int skillPoint;
        public int H, W;
        public char[] map;
        public int[] ninjas;
        public int[] dogs;
        public int[] items;
        public int[] skillUsedCount;
    }

    internal enum Skill
    {
        SPEED_UP = 0,
        PUT_OBJECT = 1,
        PUT_OBJECT_ENEMY = 2,
        DELETE_OBJECT = 3,
        DELETE_OBJECT_ENEMY = 4,
        PUT_DECOY = 5,
        PUT_DECOY_ENEMY = 6,
        HOLY_IMMOLATION = 7,
    }

    internal class Output
    {
        public string skill = null;
        public string[] moves = { "", ""};

        public string ToOutputString()
        {
            StringBuilder sb = new StringBuilder();
            if (skill != null) {
                sb.AppendLine("3");
                sb.AppendLine(skill);
            } else {
                sb.AppendLine("2");
            }
            sb.AppendLine(moves[0]);
            sb.AppendLine(moves[1]);
            return sb.ToString();
        }
    }
}
