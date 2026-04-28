import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Obstaculo {
    public static final int CACTO_PEQUENO = 0;
    public static final int CACTO_ALTO = 1;
    public static final int CACTO_DUPLO = 2;
    public static final int AVE_ALTA = 3;
    public static final int AVE_BAIXA = 4;

    private int x;
    private final int y;
    private final int largura;
    private final int altura;
    private final int tipo;
    private boolean pontuado = false;

    public Obstaculo(int x, int y, int largura, int altura, int tipo) {
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
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

    public boolean isPontuado() {
        return pontuado;
    }

    public void setPontuado(boolean pontuado) {
        this.pontuado = pontuado;
    }

    private boolean nearMissAwarded = false;

    public boolean isNearMissAwarded() {
        return nearMissAwarded;
    }

    public void setNearMissAwarded(boolean nearMissAwarded) {
        this.nearMissAwarded = nearMissAwarded;
    }

    public void desenhar(Graphics2D g2) {
        if (tipo == AVE_ALTA || tipo == AVE_BAIXA) {
            g2.setColor(Color.BLACK);

            g2.fillRect(x + 6, y + 8, 16, 8);
            g2.fillRect(x + 20, y + 4, 8, 8);
            g2.fillRect(x + 28, y + 6, 6, 3);
            g2.fillRect(x + 10, y + 5, 8, 4);
            g2.fillRect(x + 8, y + 9, 10, 3);
            g2.fillRect(x + 2, y + 10, 4, 3);

            g2.setColor(Color.WHITE);
            g2.fillRect(x + 23, y + 6, 2, 2);
            return;
        }

        g2.setColor(Color.BLACK);

        if (tipo == CACTO_PEQUENO) {
            g2.fillRect(x + 6, y, 8, altura);
            g2.fillRect(x + 1, y + 10, 5, 10);
            g2.fillRect(x + 14, y + 14, 5, 10);
            g2.fillRect(x + 4, y + altura - 4, 12, 4);
            return;
        }

        if (tipo == CACTO_ALTO) {
            g2.fillRect(x + 8, y, 9, altura);
            g2.fillRect(x + 2, y + 12, 6, 12);
            g2.fillRect(x + 17, y + 18, 6, 12);
            g2.fillRect(x + 5, y + altura - 4, 15, 4);
            return;
        }

        if (tipo == CACTO_DUPLO) {
            g2.fillRect(x + 5, y + 6, 8, 24);
            g2.fillRect(x + 15, y, 8, 30);
            g2.fillRect(x + 25, y + 8, 8, 22);
            g2.fillRect(x + 12, y + 10, 4, 8);
            g2.fillRect(x + 23, y + 12, 4, 8);
            g2.fillRect(x + 4, y + altura - 4, 28, 4);
            return;
        }

        g2.fillRect(x, y, largura, altura);
    }
}