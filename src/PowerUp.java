import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class PowerUp {
    public static final int SHIELD = 0;
    public static final int SLOW = 1;
    public static final int MULT = 2;

    private int x;
    private final int y;
    private final int largura = 20;
    private final int altura = 20;
    private final int tipo;
    private boolean coletado = false;

    public PowerUp(int x, int y, int tipo) {
        this.x = x;
        this.y = y;
        this.tipo = tipo;
    }

    public void atualizar(int velocidadeJogo) {
        x -= velocidadeJogo;
    }

    public boolean saiuDaTela() {
        return x + largura < 0;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, largura, altura);
    }

    public int getTipo() {
        return tipo;
    }

    public boolean isColetado() {
        return coletado;
    }

    public void setColetado(boolean c) {
        this.coletado = c;
    }

    public void desenhar(Graphics2D g2) {
        if (coletado) return;
        switch (tipo) {
            case SHIELD:
                g2.setColor(new Color(100, 180, 255));
                break;
            case SLOW:
                g2.setColor(new Color(180, 255, 180));
                break;
            case MULT:
                g2.setColor(new Color(255, 220, 100));
                break;
            default:
                g2.setColor(Color.MAGENTA);
        }
        g2.fillOval(x, y, largura, altura);
        g2.setColor(Color.BLACK);
        g2.drawOval(x, y, largura, altura);
    }
}