package chessFive;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Model {
	// 19行
	public static final int BOARDSIZE = 19;
	// 棋子颜色
	public static final int EMPTY = 0;
	public static final int BLACK = 1;
	public static final int WHITE = -1;
	// 游戏状态,0未开始,1进行中,2已结束
	private int status = 0;
	// 复盘
	private int replayIndex = -1;
	private boolean isReplaying = false;
	// 棋盘19x19
	private int[][] board = new int[BOARDSIZE][BOARDSIZE];
	// 自己颜色
	private int myColor = EMPTY;
	// 当前回合
	private boolean myTurn = false;
	// 计时
	private int blackTime = 60;
	private int whiteTime = 60;
	// 下棋记录
	private List<int[]> Record = new ArrayList<>();
	// 赢家
	private int winner = EMPTY;

	// 单例模式
	private Model() {
		reset();// 先重置一下
	}

	private static Model instance = null;

	public static Model getInstance() {
		if (instance == null) {
			instance = new Model();
		}
		return instance;
	}

	// 获取棋子（这个只有位置没带颜色，注意）
	public int getChess(int row, int col) {
		if (row < 0 || row >= BOARDSIZE || col < 0 || col >= BOARDSIZE)
			return EMPTY;
		return board[row][col];
	}

	// 下棋
	public boolean putChess(int row, int col, int color) {
		if (row < 0 || row >= BOARDSIZE || col < 0 || col >= BOARDSIZE
				|| board[row][col] != EMPTY || status != 1
				|| (color != BLACK && color != WHITE) || isReplaying) {
			return false;
		}
		board[row][col] = color;
		// 记录下的棋
		Record.add(new int[] { row, col, color });
		// 重置时间
		if (color == BLACK) {
			whiteTime = 60;
		} else {
			blackTime = 60;
		}
		updateTimer();
		return true;
	}

	// 判断胜者
	public int Winner(int row, int col) {
		if (row < 0 || row >= BOARDSIZE || col < 0 || col >= BOARDSIZE) {
			return EMPTY;
		}
		int color = board[row][col];
		if (color == EMPTY) {
			return EMPTY;
		}

		// 四个方向检测
		int[][] dirs = { { 1, 0 }, { 0, 1 }, { 1, 1 }, { 1, -1 } };
		for (int[] dir : dirs) {
			int count = 1;
			// 正向
			int r = row + dir[0], c = col + dir[1];
			while (r >= 0 && r < BOARDSIZE && c >= 0 && c < BOARDSIZE
					&& board[r][c] == color) {
				count++;
				r += dir[0];
				c += dir[1];
				if (count == 5)
					break;
			}
			// 反向同上
			r = row - dir[0];
			c = col - dir[1];
			while (r >= 0 && r < BOARDSIZE && c >= 0 && c < BOARDSIZE
					&& board[r][c] == color) {
				count++;
				r -= dir[0];
				c -= dir[1];
				if (count >= 5)
					break;
			}
			if (count == 5)
				return color;
		}
		return EMPTY;
	}

	// 悔棋
	public void Undo() {
		if (!Record.isEmpty() && status == 1 && !isReplaying) {
			int[] chess = Record.remove(Record.size() - 1);// 把刚下的去掉
			board[chess[0]][chess[1]] = EMPTY;
			// 悔棋后重下
			setMyTurn(true);
			blackTime = 60;
			whiteTime = 60;
		}
	}

	// 重置
	public synchronized void reset() {
		isReplaying = false;
		replayIndex = -1;
		resetBoard();
		status = 0;
		myColor = EMPTY;
		myTurn = false;
		blackTime = 60;
		whiteTime = 60;
		Record.clear();
		winner = EMPTY;
	}

	// 清空棋盘，确保没有残留
	private void resetBoard() {
		for (int i = 0; i < BOARDSIZE; i++) {
			for (int j = 0; j < BOARDSIZE; j++) {
				board[i][j] = EMPTY;
			}
		}
	}

	// 复盘
	public void startReplay() {
		if (Record.isEmpty()) {
			return;}
		isReplaying = true;
		replayIndex = 0;
		update();
	}

	public void next() {
		if (!isReplaying || replayIndex >= Record.size() - 1) {
			return;	}
		replayIndex++;
		update();
	}

	public void prev() {
		if (!isReplaying || replayIndex <= 0)
			return;
		replayIndex--;
		update();
	}

	// 退出复盘（不退出程序就卡死了)
	public synchronized void endReplay() {
		if (!isReplaying) {
			return;
		}
		isReplaying = false;
		replayIndex = -1;
		resetBoard(); // 先清空，否则就卡死了
		for (int[] move : Record) {
			board[move[0]][move[1]] = move[2];
		}
	}

	// 用于更新复盘的棋子
	private synchronized void update() {
		
		resetBoard();
		for (int i = 0; i <= replayIndex; i++) {
			int[] move = Record.get(i);
			board[move[0]][move[1]] = move[2];// 对应行列颜色
		}
	}

	// 计时器更新
	public synchronized void updateTimer() {
		if (status != 1 || isReplaying) {
			return;
		}
		if (myTurn) {// 自己回合计时
			if (myColor == BLACK)
				blackTime = Math.max(0, blackTime - 1);
			else
				whiteTime = Math.max(0, whiteTime - 1);
		}
	}

	// Getter/Setter们
	public synchronized int getStatus() {
		return status;
	}

	public synchronized void setStatus(int status) {
		this.status = status;
	}

	public synchronized int getMyColor() {
		return myColor;
	}

	public synchronized void setMyColor(int myColor) {
		this.myColor = myColor;
	}

	public synchronized boolean isMyTurn() {
		return myTurn;
	}

	public synchronized void setMyTurn(boolean myTurn) {
		this.myTurn = myTurn;
	}

	public synchronized int getBlackTime() {
		return blackTime;
	}

	public synchronized int getWhiteTime() {
		return whiteTime;
	}

	public synchronized int getWinner() {
		return winner;
	}

	public synchronized void setWinner(int winner) {
		this.winner = winner;
	}

	public synchronized List<int[]> getRecord() {
		return Record;
	}

	public synchronized int getReplayIndex() {
		return replayIndex;
	}

	public synchronized boolean isReplaying() {
		return isReplaying;
	}
}