package chessFive;

import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JOptionPane;

public class Cont {
	private Server server;
	private Client client;
	private Model model = Model.getInstance();
	private View view = View.getInstance();
	private Timer timer;

	// 初始化计时器
	private void initTimer() {
		timer = new Timer();
		//每秒更新
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				model.updateTimer();
				view.updateTimerLabel();
				checkTimeOut();
			}
		}, 1000, 1000);
	}

	//超时游戏结束
	private void checkTimeOut() {
		if (model.getBlackTime() <= 0)
			endGame(Model.WHITE);
		else if (model.getWhiteTime() <= 0)
			endGame(Model.BLACK);
	}

	// 点击
	public synchronized void Click(int row, int col) {
		if (model.isReplaying()) {
			view.showStatus("复盘模式下无法落子");
			return;
		}
		if (model.getStatus() != 1) {
			view.showStatus("游戏未开始，无法落子");
			return;
		}
		if (!model.isMyTurn()) {
			view.showStatus("等待对方操作，无法落子");
			return;
		}
		if (model.getChess(row, col) != Model.EMPTY) {
			view.showStatus("此处已被占用");
			return;
		}

		boolean success = model.putChess(row, col, model.getMyColor());
		if (success) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					view.repaint();//刷新，落子
				}
			});

			// 发送信息给对方
			send(String.format("MOVE|%d|%d|%d", row, col, model.getMyColor()));

			// 判断胜负
			int winner = model.Winner(row, col);
			if (winner != Model.EMPTY)
				endGame(winner);
			else {//没人赢继续
				model.setMyTurn(false);
				view.showStatus("等待对方操作");
			}
		}
	}

	// 发送聊天
	public void sendChatMessage(String content) {
		if (content == null || content.trim().isEmpty())
			return;
		String sender = model.getMyColor() == Model.BLACK ? "黑方" : "白方";
		send(String.format("CHAT|%s|%s", sender, content));
		//显示聊天消息
		view.addChatMessage(sender, content);
	}

	// 悔棋
	public void requestUndo() {
		if (server == null && client == null) {
			view.showStatus("未连接，无法悔棋");
			return;
		}
		if (model.isReplaying()) {
			view.showStatus("复盘模式下无法悔棋");
			return;
		}
		send("UNDO");
		view.showStatus("已发送悔棋请求");
	}

	// 解析网络消息
	private void parseMsg(String msg) {
		if (msg == null || msg.trim().isEmpty())
			return;
		String[] parts = msg.split("\\|", 4);//按|分割消息
		if (parts.length == 0)
			return;
		if ("MOVE".equals(parts[0])) {
			if (model.isReplaying()){
				return;//复盘的时候不能下棋
				}
			try {
				int row = Integer.parseInt(parts[1]);
				int col = Integer.parseInt(parts[2]);
				int color = Integer.parseInt(parts[3]);
				if (model.putChess(row, col, color)) {
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							view.repaint();
							model.setMyTurn(true);
							view.showStatus("轮到你落子");
						}
					});
					int winner = model.Winner(row, col);
					if (winner != Model.EMPTY)
						endGame(winner);
				}
			} catch (Exception e) {
				view.showStatus("落子消息解析失败：" + e.getMessage());
			}
		} else if ("CHAT".equals(parts[0])) {
			if (parts.length >= 3) {
				final String sender = parts[1];
				final String content = parts[2];
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						view.addChatMessage(sender, content);//根据发送者和内容发送消息
					}
				});
			}
		} else if ("UNDO".equals(parts[0])) {
			if (model.isReplaying()){
				return;
				}
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					int res = JOptionPane.showConfirmDialog(null, "对方请求悔棋，同意吗？",
							"悔棋请求", JOptionPane.YES_NO_OPTION);
					if (res == JOptionPane.YES_OPTION) {
						model.Undo();
                        view.repaint();
                        model.setMyTurn(true);
                        //回复对方一条消息
                        String undoReply = JOptionPane.showInputDialog(null, "跟对手说点什么吧：",
                                "回复", JOptionPane.PLAIN_MESSAGE);
                        if (undoReply != null && !undoReply.trim().isEmpty()) {
                            sendChatMessage(undoReply);
                        }
                        // 发送悔棋确认
                        send("UNDO_CONFIRM");
                        view.showStatus("已同意悔棋，你的回合");
					} else {
						view.showStatus("对方不同意悔棋");
						//回复对方一条消息
						String undoReply = JOptionPane.showInputDialog(null, "跟对手说点什么吧：",
                                "回复", JOptionPane.PLAIN_MESSAGE);
                        if (undoReply != null && !undoReply.trim().isEmpty()) {
                            sendChatMessage(undoReply);
                        }
						//发送拒绝消息
						send("UNDO_REFUSE");
					}
				}
			});
		} else if ("UNDO_CONFIRM".equals(parts[0])) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					model.Undo();
					view.repaint();
					model.setMyTurn(true);
					view.showStatus("对方同意悔棋，已回退一步");
				}
			});
		} else if ("UNDO_REFUSE".equals(parts[0])) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					view.showStatus("对方拒绝悔棋");
				}
			});
		} else if ("GAME_OVER".equals(parts[0])) {
			int winner = Integer.parseInt(parts[1]);
			model.setStatus(2);
			model.setWinner(winner);
			view.showStatus((winner == Model.BLACK ? "黑方" : "白方") + "获胜，游戏结束");
		}
	}

	// 发送网络消息
	private void send(String msg) {
		if (server != null && server.isConnected())
			{
			server.send(msg);
			}
		else if (client != null && client.isConnected())
			client.send(msg);
		else
			view.showStatus("未连接，消息发送失败");
	}

	// 结束游戏
	private void endGame(int winner) {
		model.setStatus(2);
		model.setWinner(winner);
		view.showStatus((winner == Model.BLACK ? "黑方" : "白方") + "超时获胜，游戏结束");
		send(String.format("GAME_OVER|%d", winner));
	}

	// 关闭网络连接
	public synchronized void closeNetwork() {
		if (timer != null) {
			timer.cancel();
			timer.purge();
			timer = null;
		}
		if (server != null) {
			server.close();
			server = null;
		}
		if (client != null) {
			client.close();
			client = null;
		}
		model.setStatus(0);
		// 重新初始化计时器
		initTimer();
		view.showStatus("网络连接已关闭，可重新创建服务器");
	}

	// 设置服务器
	public void setServer(final Server server) {
		this.server = server;
		new Thread(new Runnable() {
			@Override
			public void run() {
				String msg;
				try {
					while ((msg = server.receive()) != null) {
						parseMsg(msg);
					}
				} catch (Exception e) {
					view.showStatus("服务器连接断开：" + e.getMessage());
					closeNetwork();
				}
			}
		}).start();
	}

	// 设置客户端
	public void setClient(final Client client) {
		this.client = client;
		new Thread(new Runnable() {
			@Override
			public void run() {
				String msg;
				try {
					while ((msg = client.receive()) != null) {
						parseMsg(msg);
					}
				} catch (Exception e) {
					view.showStatus("客户端连接断开：" + e.getMessage());
					closeNetwork();
				}
			}
		}).start();
	}

	// 单例模式
	private Cont() {
		initTimer(); // 初始化计时器
	}

	private static Cont instance = null;

	public static Cont getInstance() {
		if (instance == null) {
			instance = new Cont();
		}
		return instance;
	}
}