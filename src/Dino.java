import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

public class Dino {

    private final int x;
    private int y;
    private final int largura;
    private final int altura;

    private double velocidadeY = 0;
    private boolean pulando = false;
    private boolean agachado = false;
    private final int alturaAgachado = 30;
    private int contadorAnimacao = 0;
    private boolean poseAlternada = false;

    public Dino() {
        this.x = GameConfig.DINO_X;
        this.largura = GameConfig.DINO_LARGURA;
        this.altura = GameConfig.DINO_ALTURA;
        this.y = GameConfig.CHAO_Y - altura;
    }

    public void atualizar() {
        if (pulando || y < GameConfig.CHAO_Y - getAlturaAtual()) {
            velocidadeY += GameConfig.GRAVIDADE;
            y += (int) velocidadeY;

            if (y >= GameConfig.CHAO_Y - getAlturaAtual()) {
                y = GameConfig.CHAO_Y - getAlturaAtual();
                velocidadeY = 0;
                pulando = false;
            }
        }

        if (!pulando && y >= GameConfig.CHAO_Y - getAlturaAtual()) {
            contadorAnimacao++;

            if (contadorAnimacao >= 6) {
                poseAlternada = !poseAlternada;
                contadorAnimacao = 0;
            }
        }
    }

    public void pular() {
        if (!pulando && y >= GameConfig.CHAO_Y - getAlturaAtual()) {
            velocidadeY = GameConfig.FORCA_PULO;
            pulando = true;
            agachado = false;
        }
    }

    public void reiniciar() {
        agachado = false;
        y = GameConfig.CHAO_Y - getAlturaAtual();
        velocidadeY = 0;
        pulando = false;
        contadorAnimacao = 0;
        poseAlternada = false;
    }

    private int getAlturaAtual() {
        return agachado ? alturaAgachado : altura;
    }

    public void setAgachado(boolean agachado) {
        if (pulando) {
            return;
        }

        this.agachado = agachado;
        y = GameConfig.CHAO_Y - getAlturaAtual();
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, largura, getAlturaAtual());
    }

    public void desenhar(Graphics2D g2) {
        g2.setColor(Color.BLACK);

        if (agachado) {
            g2.fillRect(x + 8, y + 12, 28, 14);
            g2.fillRect(x + 24, y + 4, 14, 12);
            g2.fillRect(x + 34, y + 8, 8, 3);

            g2.setColor(Color.WHITE);
            g2.fillRect(x + 30, y + 7, 4, 4);
            g2.setColor(Color.BLACK);
            g2.fillRect(x + 31, y + 8, 2, 2);

            if (poseAlternada) {
                g2.fillRect(x + 9, y + 24, 7, 6);
                g2.fillRect(x + 23, y + 23, 8, 7);
            } else {
                g2.fillRect(x + 10, y + 23, 8, 7);
                g2.fillRect(x + 24, y + 24, 7, 6);
            }
            g2.fillRect(x + 18, y + 18, 5, 4);

            Polygon cauda = new Polygon();
            cauda.addPoint(x + 8, y + 16);
            cauda.addPoint(x, y + 12);
            cauda.addPoint(x + 7, y + 22);
            g2.fillPolygon(cauda);
            return;
        }

        // corpo
        g2.fillRect(x + 10, y + 15, 24, 22);

        // cabeça
        g2.fillRect(x + 20, y, 18, 18);

        // boca
        g2.fillRect(x + 34, y + 10, 8, 4);

        // olho
        g2.setColor(Color.WHITE);
        g2.fillRect(x + 30, y + 4, 4, 4);
        g2.setColor(Color.BLACK);
        g2.fillRect(x + 31, y + 5, 2, 2);

        // pernas
        if (pulando) {
            g2.fillRect(x + 12, y + 37, 6, 13);
            g2.fillRect(x + 26, y + 37, 6, 13);
        } else if (poseAlternada) {
            g2.fillRect(x + 12, y + 36, 6, 14);
            g2.fillRect(x + 26, y + 39, 6, 11);
        } else {
            g2.fillRect(x + 12, y + 39, 6, 11);
            g2.fillRect(x + 26, y + 36, 6, 14);
        }

        // braços
        g2.fillRect(x + 7, y + 20, 6, 5);

        // cauda
        Polygon cauda = new Polygon();
        cauda.addPoint(x + 10, y + 20);
        cauda.addPoint(x, y + 15);
        cauda.addPoint(x + 8, y + 28);
        g2.fillPolygon(cauda);
    }
}