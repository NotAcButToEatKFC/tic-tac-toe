import java.awt.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Scanner;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import jdk.internal.platform.Container;

/**
 * A client for a multi-player tic tac toe game. Loosely based on an example in
 * Deitel and Deitel's "Java How to Program" book. For this project I created a
 * new application-level protocol called TTTP (for Tic Tac Toe Protocol), which
 * is entirely plain text. The messages of TTTP are:
 *
 * Client -> Server MOVE <n> QUIT
 *
 * 
 * Server -> Client WELCOME <char> VALID_MOVE OTHER_PLAYER_MOVED <n>
 * OTHER_PLAYER_LEFT VICTORY DEFEAT TIE MESSAGE <text>
 */
public class TicTacToeClient extends Thread {

	static TicTacToeClient client;

	private JFrame frame = new JFrame("Tic Tac Toe");
	private JLabel messageLabel = new JLabel("...");

	private Square[] board = new Square[9];
	private Square currentSquare;

	private Socket socket;
	private Scanner in;
	private PrintWriter out;

	private static String gameMode = "";
	private static int gameDifficulty = 0;
	private static boolean isWindowClosed;

	public TicTacToeClient(String serverAddress) throws Exception {

		socket = new Socket(serverAddress, 58901);
		in = new Scanner(socket.getInputStream());
		out = new PrintWriter(socket.getOutputStream(), true);

		messageLabel.setBackground(Color.lightGray);
		frame.getContentPane().add(messageLabel, BorderLayout.SOUTH);

		JPanel boardPanel = new JPanel();
		boardPanel.setBackground(Color.black);
		boardPanel.setLayout(new GridLayout(3, 3, 2, 2));
		for (int i = 0; i < board.length; i++) {
			final int j = i;
			board[i] = new Square();
			board[i].addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					currentSquare = board[j];
					out.println("MOVE " + j);
				}
			});
			boardPanel.add(board[i]);
		}
		frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
	}

	/**
	 * The main thread of the client will listen for messages from the server. The
	 * first message will be a "WELCOME" message in which we receive our mark. Then
	 * we go into a loop listening for any of the other messages, and handling each
	 * message appropriately. The "VICTORY", "DEFEAT", "TIE", and
	 * "OTHER_PLAYER_LEFT" messages will ask the user whether or not to play another
	 * game. If the answer is no, the loop is exited and the server is sent a "QUIT"
	 * message.
	 */
	public void run() {
		System.out.println("start game");
		try {
			out.println("GAMEMODE " + gameMode + " " + gameDifficulty);
			if (gameMode == "PVC") {
				out.println("GAMEDIFFICULTY " + gameDifficulty);
			}
			String response = in.nextLine();
			char mark = response.charAt(8);
			char opponentMark = mark == 'X' ? 'O' : 'X';
			frame.setTitle("Tic Tac Toe: Player " + mark);
			while (in.hasNextLine() && !isWindowClosed) {
				response = in.nextLine();
				if (response.startsWith("VALID_MOVE")) {
					messageLabel.setText("Valid move, please wait");
					currentSquare.setText(mark);
					currentSquare.repaint();
				} else if (response.startsWith("OPPONENT_MOVED")) {
					int loc = Integer.parseInt(response.substring(15));
					board[loc].setText(opponentMark);
					board[loc].repaint();
					messageLabel.setText("Opponent moved, your turn");
				} else if (response.startsWith("MESSAGE")) {
					messageLabel.setText(response.substring(8));
				} else if (response.startsWith("VICTORY")) {
					JOptionPane.showMessageDialog(frame, "Winner Winner");
					break;
				} else if (response.startsWith("DEFEAT")) {
					JOptionPane.showMessageDialog(frame, "Sorry you lost");
					break;
				} else if (response.startsWith("TIE")) {
					JOptionPane.showMessageDialog(frame, "Tie");
					break;
				} else if (response.startsWith("OTHER_PLAYER_LEFT")) {
					JOptionPane.showMessageDialog(frame, "Other player left");
					break;
				}
			}
			System.out.println("player quit");
			out.println("QUIT");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Player thread finally execute.");
			try {
				socket.close();
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
			frame.dispose();
		}
	}

	static class Square extends JPanel {
		private static final long serialVersionUID = 1L;
		JLabel label = new JLabel();

		public Square() {
			setBackground(Color.white);
			setLayout(new GridBagLayout());
			label.setFont(new Font("Arial", Font.BOLD, 40));
			add(label);
		}

		public void setText(char text) {
			label.setForeground(text == 'X' ? Color.BLUE : Color.RED);
			label.setText(text + "");
		}
	}

	static void startGame(final String address) throws Exception {
		client = new TicTacToeClient(address);
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setSize(320, 320);
		client.frame.setResizable(false);
		client.frame.setVisible(true);// 显示游戏界面
		client.frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("game windows is closing");
				isWindowClosed = true;
				client.out.println("QUIT");
			}
		});
		isWindowClosed = false;
		client.start();

	}

	static class startFrame {

		public startFrame(final String address) {

			final JFrame startUp = new JFrame("井字棋");
			startUp.setSize(300, 300);
			startUp.setResizable(false);
			startUp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			startUp.getContentPane().setLayout(new GridLayout(5, 1));
			startUp.invalidate();

			JLabel gameName = new JLabel("井字棋");

			String pvp = "人人对战";
			final JButton pvpbutton = new JButton(pvp);
			pvpbutton.setMnemonic(KeyEvent.VK_B);
			pvpbutton.setActionCommand(pvp);

			String pvc = "人机对战";
			final JButton pvcbutton = new JButton(pvc);
			pvcbutton.setMnemonic(KeyEvent.VK_B);
			pvcbutton.setActionCommand(pvc);

			ButtonGroup group = new ButtonGroup();
			group.add(pvcbutton);

			JPanel gamename = new JPanel();
			JPanel gameMode1 = new JPanel();
			JPanel gameMode2 = new JPanel();
			final JPanel difficultyLevels = new JPanel();

			gamename.add(gameName);
			startUp.getContentPane().add(gamename, BorderLayout.CENTER);
			gameMode1.add(pvpbutton);
			startUp.getContentPane().add(gameMode1, BorderLayout.CENTER);
			gameMode2.add(pvcbutton);
			startUp.getContentPane().add(gameMode2, BorderLayout.CENTER);

			JLabel selectDiffTips = new JLabel("请选择难度");
			final JPanel sft = new JPanel();
			sft.add(selectDiffTips);
			startUp.getContentPane().add(sft, BorderLayout.CENTER);
			sft.setVisible(false);

			final JButton easy = new JButton("简单");
			final JButton difficult = new JButton("困难");

			difficultyLevels.add(easy);
			difficultyLevels.add(difficult);
			startUp.getContentPane().add(difficultyLevels, BorderLayout.CENTER);

			difficultyLevels.setVisible(false);
			startUp.setVisible(true);
			pvpbutton.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					System.out.println("pvp button click");
					gameMode = "PVP";
					startUp.setVisible(false);// 模式选择界面隐藏

					try {
						startGame(address);
					} catch (Exception e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			});
			pvcbutton.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					System.out.println("pvc button click");
					gameMode = "PVC";
					sft.setVisible(true);
					difficultyLevels.setVisible(true);
				}
			});

			easy.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					System.out.println("difficulty: easy");
					gameDifficulty = 1;
					startUp.setVisible(false);// 模式选择界面隐藏
					try {
						startGame(address);
					} catch (Exception e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			});

			difficult.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					System.out.println("difficulty: difficult");
					gameDifficulty = 3;
					startUp.setVisible(false);// 模式选择界面隐藏
					try {
						startGame(address);
					} catch (Exception e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			});

		}

	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Pass the server IP as the sole command line argument");
			return;
		}

		startFrame startWindow = new startFrame(args[0]);

	}
}