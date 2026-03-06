package chessFive;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class View extends JFrame {
	private JPanel boardPanel;// 棋盘
	private JButton createSerButton;// 创建服务器
	private JButton connectServerButton;// 连接服务器
	private JButton undoButton;// 悔棋
	private JButton resetButton;// 重置
	private JButton startReplayBtn;// 开始复盘
	private JButton prevStepBtn;// 下一步
	private JButton nextStepBtn;// 上一步
	private JButton exitReplayBtn; // 退出复盘按钮
	private JLabel statusLabel;// 状态显示
	private JLabel timerLabel;// 计时显示
	private JTextArea chatArea;// 聊天记录
	private JTextField chatInput;// 聊天输入
	private final int GRID_SIZE = 30; // 格子大小
	private final int MARGIN = 30; // 边距
	private final int CHESS_SIZE = 26; // 棋子大小

	// 游戏界面
	private void Window() {
		super.setTitle("五子棋");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));
		setSize(900, 800);

		// 顶部状态栏
		JPanel topPanel = new JPanel(new BorderLayout());
		statusLabel = new JLabel("请创建服务器或连接服务器", JLabel.CENTER);
		timerLabel = new JLabel("黑方:60s|白方:60s", JLabel.CENTER);
		topPanel.add(statusLabel, BorderLayout.NORTH);
		topPanel.add(timerLabel, BorderLayout.SOUTH);

		// 棋盘
		boardPanel = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				drawBoard(g);
				drawChess(g);
			}
		};
		boardPanel.setBackground(Color.WHITE);

		// 点击事件
		boardPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				// 下在交叉点上
				int col = (int) Math.round((x - MARGIN) / (double) GRID_SIZE);
				int row = (int) Math.round((y - MARGIN) / (double) GRID_SIZE);
				if (row >= 0 && row < Model.BOARDSIZE && col >= 0
						&& col < Model.BOARDSIZE) {
					Cont.getInstance().Click(row, col);// 下棋
				} else {
					showStatus("坐标超出范围，请选择有效位置");
				}
			}
		});

		// 聊天
		JPanel chatPanel = initChatPanel();

		// 整合
		JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
		mainPanel.add(boardPanel, BorderLayout.CENTER);
		mainPanel.add(chatPanel, BorderLayout.EAST);

		// 按钮
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		createSerButton = new JButton("创建服务器");
		connectServerButton = new JButton("连接服务器");
		undoButton = new JButton("悔棋");
		resetButton = new JButton("重置");
		startReplayBtn = new JButton("开始复盘");
		prevStepBtn = new JButton("上一步");
		nextStepBtn = new JButton("下一步");
		exitReplayBtn = new JButton("退出复盘");

		// 创建服务器
		createSerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Cont.getInstance().closeNetwork();
				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Server server = new Server(9999);
							showStatus("服务器创建成功，等待连接");
							server.acceptClient();
							Cont.getInstance().setServer(server);
							Model.getInstance().setMyColor(Model.BLACK);
							Model.getInstance().setStatus(1);
							Model.getInstance().setMyTurn(true);
							showStatus("连接成功，你是黑方");
						} catch (Exception e2) {
							showStatus("创建服务器失败：" + e2.getMessage());
						}
					}
				}).start();
			}
		});

		// 连接服务器
		connectServerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Cont.getInstance().closeNetwork();
				final String ip = JOptionPane.showInputDialog("请输入ip",
						"127.0.0.1");
				if (ip == null || ip.trim().isEmpty()) {
					return;
				}
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Client client = new Client(ip.trim(), 9999);
							Cont.getInstance().setClient(client);
							Model.getInstance().setMyColor(Model.WHITE);
							Model.getInstance().setStatus(1);
							showStatus("连接成功，你是白方");
						} catch (Exception e2) {
							showStatus("连接服务器失败：" + e2.getMessage());
						}
					}
				}).start();
			}
		});

		// 悔棋
		undoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Cont.getInstance().requestUndo();
			}
		});

		// 重置
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							Model.getInstance().endReplay();
							// 匿名内部类创建新线程
							new Thread(new Runnable() {
								@Override
								public void run() {
									Cont.getInstance().closeNetwork();
								}
							}).start();// 启动新的线程，否则会卡死
							Model.getInstance().reset();
							repaint();
							timerLabel.setText("黑方:60s | 白方:60s");
							chatArea.setText("");
							showStatus("重置成功，请重新开始");
						} catch (Exception ex) {
							ex.printStackTrace();
							showStatus("重置失败：" + ex.getMessage());
						}
					}
				});
			}
		});

		// 开始复盘
		startReplayBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Model model = Model.getInstance();
				if (model.getRecord().isEmpty()) {
					showStatus("无落子记录，无法复盘");
					return;
				}
				model.startReplay();
				repaint();
				showStatus("复盘开始（第1步）");
			}
		});

		// 上一步复盘
		prevStepBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Model model = Model.getInstance();
				if (!model.isReplaying()) {
					showStatus("未开始复盘");
					return;
				}
				model.prev();
				repaint();
				showStatus("复盘第" + (model.getReplayIndex() + 1) + "步");
			}
		});

		// 下一步复盘
		nextStepBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Model model = Model.getInstance();
				if (!model.isReplaying()) {
					showStatus("未开始复盘");
					return;
				}
				model.next();
				repaint();
				int total = model.getRecord().size();
				showStatus("复盘第" + (model.getReplayIndex() + 1) + "步（共" + total
						+ "步）");
			}
		});

		// 退出复盘
		exitReplayBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Model model = Model.getInstance();
						if (!model.isReplaying()) {
							showStatus("未处于复盘模式");
							return;
						}
						model.endReplay();
						repaint();
						showStatus("已退出复盘，可正常游戏");
					}
				});
			}
		});

		// 添加按钮到面板
		btnPanel.add(createSerButton);
		btnPanel.add(connectServerButton);
		btnPanel.add(undoButton);
		btnPanel.add(resetButton);
		btnPanel.add(startReplayBtn);
		btnPanel.add(prevStepBtn);
		btnPanel.add(nextStepBtn);
		btnPanel.add(exitReplayBtn);

		// 组装窗口
		add(topPanel, BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);
		add(btnPanel, BorderLayout.SOUTH);
		setLocationRelativeTo(null);
	}

	// 初始化聊天面板
	private JPanel initChatPanel() {
		chatArea = new JTextArea(25, 18);
		chatArea.setEditable(false);
		chatArea.setBorder(BorderFactory.createTitledBorder("聊天"));
		chatArea.setLineWrap(true);
		chatArea.setWrapStyleWord(true);

		chatInput = new JTextField(15);
		JButton sendBtn = new JButton("发送");

		// 发送聊天
		sendBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendChat();
			}
		});

		// 回车发送
		chatInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendChat();
					e.consume();//防止触发其他逻辑导致卡死
				}
			}
		});

		JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		inputPanel.add(chatInput);
		inputPanel.add(sendBtn);

		JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
		chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
		chatPanel.add(inputPanel, BorderLayout.SOUTH);
		return chatPanel;
	}

	// 发送聊天消息
	private void sendChat() {
		String content = chatInput.getText().trim();//消息去掉空格
		if (!content.isEmpty()) {
			Cont.getInstance().sendChatMessage(content);
			chatInput.setText("");
			chatInput.requestFocus();
		}
	}

	// 绘制棋盘
	private void drawBoard(Graphics g) {
		g.setColor(Color.BLACK);
		// 横线
		for (int i = 0; i < Model.BOARDSIZE; i++) {
			g.drawLine(MARGIN, MARGIN + i * GRID_SIZE, MARGIN
					+ (Model.BOARDSIZE - 1) * GRID_SIZE, MARGIN + i * GRID_SIZE);
		}
		// 竖线
		for (int i = 0; i < Model.BOARDSIZE; i++) {
			g.drawLine(MARGIN + i * GRID_SIZE, MARGIN, MARGIN + i * GRID_SIZE,
					MARGIN + (Model.BOARDSIZE - 1) * GRID_SIZE);
		}
	}

	// 绘制棋子
	private void drawChess(Graphics g) {
		Model model = Model.getInstance();
		for (int row = 0; row < Model.BOARDSIZE; row++) {
			for (int col = 0; col < Model.BOARDSIZE; col++) {
				int color = model.getChess(row, col);
				if (color == Model.EMPTY)
					continue;
				int x = MARGIN + col * GRID_SIZE - CHESS_SIZE / 2;
				int y = MARGIN + row * GRID_SIZE - CHESS_SIZE / 2;
				if (color == Model.BLACK) {
					g.setColor(Color.BLACK);
					g.fillOval(x, y, CHESS_SIZE, CHESS_SIZE);
				} else {
					g.setColor(Color.WHITE);
					g.fillOval(x, y, CHESS_SIZE, CHESS_SIZE);
					g.setColor(Color.BLACK);
					g.drawOval(x, y, CHESS_SIZE, CHESS_SIZE);
				}
			}
		}
	}

	// 显示聊天消息
	public void addChatMessage(String sender, String content) {
		chatArea.append(String.format("[%s]：%s%n", sender, content));
		chatArea.setCaretPosition(chatArea.getText().length());//消息界面滑动到最下方
	}

	// 显示状态
	public void showStatus(String msg) {
		statusLabel.setText(msg);
	}

	// 更新计时器
	public void updateTimerLabel() {
		Model model = Model.getInstance();
		timerLabel.setText(String.format("黑方:%ds | 白方:%ds",
				model.getBlackTime(), model.getWhiteTime()));
	}

	// 单例模式
	private View() {
		Window();
	}

	private static View instance = null;

	public static View getInstance() {
		if (instance == null) {
			instance = new View();
		}
		return instance;
	}

	@Override
	public void repaint() {
		boardPanel.repaint();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				View.getInstance().setVisible(true);
			}
		});
	}
}