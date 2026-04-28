import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame("Jogo Dino");
            GamePanel jogo = new GamePanel();

            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            janela.setContentPane(jogo);
            janela.pack();
            janela.setLocationRelativeTo(null);
            janela.setResizable(false);
            janela.setVisible(true);
        });
    }
}