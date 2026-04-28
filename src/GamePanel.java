import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    private enum EstadoJogo {
        MENU,
        JOGANDO,
        PAUSADO,
        GAME_OVER
    }

    private final Timer timer;
    private final Dino dino;
    private final ArrayList<Obstaculo> obstaculos;
    private final Random random;
    private final Path caminhoRecorde = Paths.get("recorde.txt");

    private EstadoJogo estado = EstadoJogo.MENU;

    private int tempoSpawn = 0;
    private int intervaloSpawn = 70;
    private int velocidadeJogo = 6;

    private double distanciaPercorrida = 0;
    private int pontuacao = 0;
    private int melhorPontuacao = 0;
    private int ultimoMarcoPontuacao = 0;

    private boolean novoRecorde = false;
    private int tempoNovoRecorde = 0;
    private int tempoMarcoPontuacao = 0;
    private int tempoFlashColisao = 0;
    private int contadorPiscaMenu = 0;

    private int comboCount = 0;
    private int multiplicador = 1;

    private final ArrayList<PowerUp> powerUps = new ArrayList<>();
    private boolean shieldActive = false;
    private int shieldTimer = 0;
    private boolean slowActive = false;
    private int slowTimer = 0;
    private boolean multActive = false;
    private int multTimer = 0;

    private final Path caminhoLeaderboard = Paths.get("leaderboard.txt");
    private boolean nomeMode = false;
    private String nomeBuffer = "";
    private int qualifyIndex = -1;
    private int pendingScore = 0;

    private int posicaoChao = 0;
    private int posicaoPoeira = 0;
    private int posicaoNuvens = 0;
    private int posicaoMontanhas = 0;
    private int ultimoTipoObstaculo = -1;

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.LARGURA_TELA, GameConfig.ALTURA_TELA));
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(this);

        this.dino = new Dino();
        this.obstaculos = new ArrayList<>();
        this.random = new Random();

        carregarRecorde();

        timer = new Timer(GameConfig.FPS_DELAY, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        contadorPiscaMenu = (contadorPiscaMenu + 1) % 60;

        if (tempoFlashColisao > 0) {
            tempoFlashColisao--;
        }

        if (estado == EstadoJogo.JOGANDO) {
            atualizarJogo();
        }

        repaint();
    }

    private void atualizarJogo() {
        dino.atualizar();

        posicaoNuvens -= 1;
        if (posicaoNuvens <= -GameConfig.LARGURA_TELA) {
            posicaoNuvens = 0;
        }

        posicaoMontanhas -= 2;
        if (posicaoMontanhas <= -GameConfig.LARGURA_TELA) {
            posicaoMontanhas = 0;
        }

        posicaoChao -= velocidadeJogo;
        if (posicaoChao <= -40) {
            posicaoChao = 0;
        }

        posicaoPoeira -= velocidadeJogo + 1;
        if (posicaoPoeira <= -120) {
            posicaoPoeira = 0;
        }

        distanciaPercorrida += velocidadeJogo * 0.10;
        int novaPontuacao = (int) distanciaPercorrida;
        if (novaPontuacao > pontuacao) {
            pontuacao = novaPontuacao;
            tratarPontuacao();
        }

        // compute base velocity and apply slow power-up if active
        int baseVel = Math.min(12, 6 + pontuacao / 120);
        if (slowActive) {
            velocidadeJogo = Math.max(3, (int) Math.round(baseVel * 0.7));
        } else {
            velocidadeJogo = baseVel;
        }

        tempoSpawn++;
        if (tempoSpawn >= intervaloSpawn) {
            criarObstaculo();
            tempoSpawn = 0;
            int baseSpawn = Math.max(GameConfig.MIN_INTERVALO_SPAWN, 90 - velocidadeJogo * 3 - pontuacao / 20);
            intervaloSpawn = baseSpawn + random.nextInt(18);
        }

        // chance to spawn a power-up
        if (random.nextDouble() < GameConfig.POWERUP_SPAWN_CHANCE) {
            criarPowerUp();
        }

        Iterator<Obstaculo> it = obstaculos.iterator();
        while (it.hasNext()) {
            Obstaculo obs = it.next();
            obs.atualizar(velocidadeJogo);

            // Award points when an obstacle is passed (clear) and wasn't yet counted
            if (!obs.isPontuado()) {
                Rectangle bounds = obs.getBounds();
                Rectangle dinoBounds = dino.getBounds();

                // near-miss bonus: brief window before fully passed
                if (!obs.isNearMissAwarded() && bounds.x + bounds.width < dinoBounds.x + 12 && bounds.x + bounds.width > dinoBounds.x - 24) {
                    pontuacao += 2; // small near-miss bonus
                    obs.setNearMissAwarded(true);
                    Toolkit.getDefaultToolkit().beep();
                    tratarPontuacao();
                }

                if (bounds.x + bounds.width < dinoBounds.x) {
                    int base = 5;
                    int tipo = obs.getTipo();
                    if (tipo == Obstaculo.CACTO_PEQUENO) base = 5;
                    else if (tipo == Obstaculo.CACTO_ALTO) base = 8;
                    else if (tipo == Obstaculo.CACTO_DUPLO) base = 12;
                    else if (tipo == Obstaculo.AVE_BAIXA) base = 10;
                    else if (tipo == Obstaculo.AVE_ALTA) base = 15;

                    pontuacao += base * multiplicador;
                    obs.setPontuado(true);

                    // increase combo and recalc multiplier (1 per 5 consecutive) and keep temporary mult
                    comboCount++;
                    multiplicador = 1 + comboCount / 5 + (multActive ? 2 : 0);

                    Toolkit.getDefaultToolkit().beep();
                    tratarPontuacao();
                }
            }

            if (obs.saiuDaTela()) {
                it.remove();
            }
        }

        // update power-ups
        Iterator<PowerUp> pit = powerUps.iterator();
        while (pit.hasNext()) {
            PowerUp p = pit.next();
            p.atualizar(velocidadeJogo);

            if (!p.isColetado() && p.getBounds().intersects(dino.getBounds())) {
                p.setColetado(true);
                aplicarPowerUp(p.getTipo());
            }

            if (p.saiuDaTela() || p.isColetado()) {
                pit.remove();
            }
        }

        if (tempoNovoRecorde > 0) {
            tempoNovoRecorde--;
        } else {
            novoRecorde = false;
        }

        if (tempoMarcoPontuacao > 0) {
            tempoMarcoPontuacao--;
        }

        // update power-up timers
        if (shieldActive) {
            shieldTimer--;
            if (shieldTimer <= 0) shieldActive = false;
        }
        if (slowActive) {
            slowTimer--;
            if (slowTimer <= 0) slowActive = false;
        }
        if (multActive) {
            multTimer--;
            if (multTimer <= 0) multActive = false;
        }

        verificarColisoes();
    }

    private void tratarPontuacao() {
        if (pontuacao >= ultimoMarcoPontuacao + 100) {
            ultimoMarcoPontuacao = (pontuacao / 100) * 100;
            tempoMarcoPontuacao = 35;
        }

        if (pontuacao > melhorPontuacao) {
            melhorPontuacao = pontuacao;
            salvarRecorde();
            novoRecorde = true;
            tempoNovoRecorde = 75;
        }
    }

    private void verificarColisoes() {
        Rectangle areaDino = dino.getBounds();

        Iterator<Obstaculo> it = obstaculos.iterator();
        while (it.hasNext()) {
            Obstaculo obs = it.next();
            if (areaDino.intersects(obs.getBounds())) {
                if (shieldActive) {
                    // consume shield and remove the obstacle so collision doesn't persist
                    shieldActive = false;
                    shieldTimer = 0;
                    tempoFlashColisao = 8;
                    Toolkit.getDefaultToolkit().beep();
                    it.remove();
                    // small score for shield used
                    pontuacao += 1;
                    tratarPontuacao();
                    // continue checking others
                    continue;
                } else {
                    estado = EstadoJogo.GAME_OVER;
                    tempoFlashColisao = 10;
                    // reset combo on collision
                    comboCount = 0;
                    multiplicador = 1;

                    // update leaderboard once (may prompt for name)
                    handleGameOverLeaderboard();
                    break;
                }
            }
        }
    }

    private void criarObstaculo() {
        int tipo;
        int tentativas = 0;

        do {
            if (pontuacao < 15) {
                tipo = random.nextInt(2);
            } else if (pontuacao < 35) {
                tipo = random.nextInt(4);
            } else {
                tipo = random.nextInt(5);
            }
            tentativas++;
        } while (deveRejeitarObstaculo(tipo) && tentativas < 12);

        switch (tipo) {
            case Obstaculo.CACTO_PEQUENO:
                obstaculos.add(new Obstaculo(
                        GameConfig.LARGURA_TELA,
                        GameConfig.CHAO_Y - 35,
                        20,
                        35,
                        Obstaculo.CACTO_PEQUENO
                ));
                break;

            case Obstaculo.CACTO_ALTO:
                obstaculos.add(new Obstaculo(
                        GameConfig.LARGURA_TELA,
                        GameConfig.CHAO_Y - 45,
                        25,
                        45,
                        Obstaculo.CACTO_ALTO
                ));
                break;

            case Obstaculo.CACTO_DUPLO:
                obstaculos.add(new Obstaculo(
                        GameConfig.LARGURA_TELA,
                        GameConfig.CHAO_Y - 30,
                        35,
                        30,
                        Obstaculo.CACTO_DUPLO
                ));
                break;

            case Obstaculo.AVE_ALTA:
                obstaculos.add(new Obstaculo(
                        GameConfig.LARGURA_TELA,
                        GameConfig.CHAO_Y - 92,
                        34,
                        20,
                        Obstaculo.AVE_ALTA
                ));
                break;

            case Obstaculo.AVE_BAIXA:
                obstaculos.add(new Obstaculo(
                        GameConfig.LARGURA_TELA,
                        GameConfig.CHAO_Y - 58,
                        34,
                        20,
                        Obstaculo.AVE_BAIXA
                ));
                break;

            default:
                break;
        }

        ultimoTipoObstaculo = tipo;
    }

    private boolean deveRejeitarObstaculo(int tipo) {
        if (ultimoTipoObstaculo == -1) {
            return false;
        }

        if (ultimoTipoObstaculo == Obstaculo.AVE_BAIXA && tipo == Obstaculo.AVE_BAIXA) {
            return true;
        }

        if (ultimoTipoObstaculo == Obstaculo.CACTO_DUPLO && tipo == Obstaculo.AVE_BAIXA) {
            return true;
        }

        if (ultimoTipoObstaculo == Obstaculo.CACTO_ALTO && tipo == Obstaculo.CACTO_ALTO && pontuacao < 50) {
            return true;
        }

        return ultimoTipoObstaculo == Obstaculo.AVE_ALTA && tipo == Obstaculo.AVE_BAIXA;
    }

    private void carregarRecorde() {
        if (Files.exists(caminhoRecorde)) {
            try {
                String texto = Files.readString(caminhoRecorde).trim();
                if (!texto.isEmpty()) {
                    melhorPontuacao = Integer.parseInt(texto);
                }
            } catch (IOException | NumberFormatException e) {
                melhorPontuacao = 0;
            }
        }
    }


    private void criarPowerUp() {
        if (powerUps.size() >= 1) return;
        int tipo = random.nextInt(3);
        int y = GameConfig.CHAO_Y - 80 + random.nextInt(50);
        powerUps.add(new PowerUp(GameConfig.LARGURA_TELA, y, tipo));
        Toolkit.getDefaultToolkit().beep();
    }

    private void aplicarPowerUp(int tipo) {
        Toolkit.getDefaultToolkit().beep();
        if (tipo == PowerUp.SHIELD) {
            shieldActive = true;
            shieldTimer = GameConfig.POWERUP_DURATION_TICKS;
        } else if (tipo == PowerUp.SLOW) {
            slowActive = true;
            slowTimer = GameConfig.POWERUP_DURATION_TICKS;
        } else if (tipo == PowerUp.MULT) {
            multActive = true;
            multTimer = GameConfig.POWERUP_DURATION_TICKS;
        }
    }

    private static class LeaderEntry {
        String name;
        int score;
        LeaderEntry(String name, int score) { this.name = name; this.score = score; }
    }

    private void carregarLeaderboardEntries(List<LeaderEntry> list) {
        list.clear();
        if (Files.exists(caminhoLeaderboard)) {
            try {
                for (String line : Files.readAllLines(caminhoLeaderboard)) {
                    if (line == null || line.trim().isEmpty()) continue;
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        try {
                            String n = parts[0];
                            int s = Integer.parseInt(parts[1].trim());
                            list.add(new LeaderEntry(n, s));
                        } catch (NumberFormatException ex) {
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    private void salvarLeaderboardEntries(List<LeaderEntry> list) {
        try {
            StringBuilder sb = new StringBuilder();
            for (LeaderEntry e : list) {
                sb.append(e.name).append(":").append(e.score).append(System.lineSeparator());
            }
            Files.writeString(caminhoLeaderboard, sb.toString());
        } catch (IOException e) {
        }
    }

    private int findInsertIndex(List<LeaderEntry> list, int score) {
        for (int i = 0; i < list.size(); i++) {
            if (score > list.get(i).score) return i;
        }
        return list.size();
    }

    private void handleGameOverLeaderboard() {
        try {
            List<LeaderEntry> list = new ArrayList<>();
            carregarLeaderboardEntries(list);
            int idx = findInsertIndex(list, pontuacao);
            if (idx < 5) {
                nomeMode = true;
                nomeBuffer = "";
                qualifyIndex = idx;
                pendingScore = pontuacao;
            } else {
                list.add(new LeaderEntry("ANON", pontuacao));
                Collections.sort(list, (a,b) -> b.score - a.score);
                if (list.size() > 5) list = list.subList(0, 5);
                salvarLeaderboardEntries(list);
            }
        } catch (Exception e) {
        }
    }

    private void salvarRecorde() {
        try {
            Files.writeString(caminhoRecorde, String.valueOf(melhorPontuacao));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void iniciarJogo() {
        dino.reiniciar();
        obstaculos.clear();

        tempoSpawn = 0;
        intervaloSpawn = 70;
        velocidadeJogo = 6;

        distanciaPercorrida = 0;
        pontuacao = 0;
        ultimoMarcoPontuacao = 0;

        posicaoChao = 0;
        posicaoPoeira = 0;
        posicaoNuvens = 0;
        posicaoMontanhas = 0;
        ultimoTipoObstaculo = -1;

        novoRecorde = false;
        tempoNovoRecorde = 0;
        tempoMarcoPontuacao = 0;
        tempoFlashColisao = 0;

        // reset combo/multiplier
        comboCount = 0;
        multiplicador = 1;

        // reset power-ups
        powerUps.clear();
        shieldActive = false;
        shieldTimer = 0;
        slowActive = false;
        slowTimer = 0;
        multActive = false;
        multTimer = 0;

        estado = EstadoJogo.JOGANDO;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        desenharCenario(g2);
        dino.desenhar(g2);
        desenharObstaculos(g2);
        desenharPowerUps(g2);
        desenharHUD(g2);

        if (estado == EstadoJogo.MENU) {
            desenharTelaInicial(g2);
        } else if (estado == EstadoJogo.PAUSADO) {
            desenharPausado(g2);
        } else if (estado == EstadoJogo.GAME_OVER) {
            desenharGameOver(g2);
        }

        if (tempoFlashColisao > 0) {
            int alpha = 30 + tempoFlashColisao * 12;
            g2.setColor(new Color(255, 255, 255, Math.min(alpha, 180)));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void desenharCenario(Graphics2D g2) {
        g2.setColor(new Color(247, 247, 247));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(226, 226, 226));
        g2.fillOval(730, 35, 54, 54);

        g2.setColor(new Color(214, 214, 214));
        for (int i = posicaoMontanhas; i < getWidth() + GameConfig.LARGURA_TELA; i += 220) {
            int[] x1 = {i, i + 60, i + 120};
            int[] y1 = {GameConfig.CHAO_Y, GameConfig.CHAO_Y - 60, GameConfig.CHAO_Y};
            g2.fillPolygon(x1, y1, 3);

            int[] x2 = {i + 80, i + 150, i + 220};
            int[] y2 = {GameConfig.CHAO_Y, GameConfig.CHAO_Y - 45, GameConfig.CHAO_Y};
            g2.fillPolygon(x2, y2, 3);
        }

        g2.setColor(new Color(223, 223, 223));
        for (int i = posicaoNuvens; i < getWidth() + GameConfig.LARGURA_TELA; i += 180) {
            g2.fillOval(i + 20, 50, 36, 16);
            g2.fillOval(i + 45, 45, 34, 18);
            g2.fillOval(i + 66, 50, 28, 14);

            g2.fillOval(i + 120, 80, 40, 16);
            g2.fillOval(i + 145, 75, 35, 18);
            g2.fillOval(i + 170, 80, 26, 14);
        }

        g2.setColor(Color.BLACK);
        g2.drawLine(0, GameConfig.CHAO_Y, getWidth(), GameConfig.CHAO_Y);

        for (int i = posicaoChao; i < getWidth() + 40; i += 40) {
            g2.drawLine(i, GameConfig.CHAO_Y + 8, i + 20, GameConfig.CHAO_Y + 8);
        }

        g2.setColor(new Color(125, 125, 125));
        for (int i = posicaoPoeira; i < getWidth() + 120; i += 120) {
            g2.drawLine(i + 20, GameConfig.CHAO_Y - 6, i + 32, GameConfig.CHAO_Y - 6);
            g2.drawLine(i + 55, GameConfig.CHAO_Y - 3, i + 64, GameConfig.CHAO_Y - 3);
            g2.drawLine(i + 83, GameConfig.CHAO_Y - 8, i + 92, GameConfig.CHAO_Y - 8);
        }
    }

    private void desenharObstaculos(Graphics2D g2) {
        for (Obstaculo obs : obstaculos) {
            obs.desenhar(g2);
        }
    }

    private void desenharPowerUps(Graphics2D g2) {
        for (PowerUp p : powerUps) {
            p.desenhar(g2);
        }
    }

    private void desenharHUD(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 215));
        int hudWidth = 640;
        int hudHeight = 60;
        g2.fillRoundRect(14, 10, hudWidth, hudHeight, 14, 14);

        g2.setColor(Color.BLACK);
        g2.drawRoundRect(14, 10, hudWidth, hudHeight, 14, 14);

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();

        // First line (top)
        int y1 = 30;
        int leftX = 28;
        String pontosText = "Pontos: " + pontuacao;
        g2.drawString(pontosText, leftX, y1);

        leftX += fm.stringWidth(pontosText) + 20;
        String recText = "Recorde: " + melhorPontuacao;
        g2.drawString(recText, leftX, y1);

        String speedText = "Velocidade: " + velocidadeJogo;
        int speedX = 14 + hudWidth / 2 - fm.stringWidth(speedText) / 2;
        g2.drawString(speedText, speedX, y1);

        // Second line (bottom)
        int y2 = 50;
        String comboText = "Combo: " + comboCount;
        g2.drawString(comboText, 28, y2);

        String mulText = "x" + multiplicador;
        g2.drawString(mulText, 28 + fm.stringWidth(comboText) + 20, y2);

        // Power-up indicators (right aligned)
        String indicators = "";
        if (shieldActive) indicators += "Shield  ";
        if (slowActive) indicators += "Slow  ";
        if (multActive) indicators += "Mult  ";
        if (!indicators.isEmpty()) {
            int indW = fm.stringWidth(indicators);
            int indX = 14 + hudWidth - 20 - indW;
            g2.drawString(indicators.trim(), indX, y2);
        }

        // Notifications
        if (novoRecorde && (tempoNovoRecorde / 6) % 2 == 0) {
            String nr = "NOVO RECORDE!";
            int nx = 14 + hudWidth - fm.stringWidth(nr) - 10;
            g2.drawString(nr, nx, y1);
        } else if (tempoMarcoPontuacao > 0 && (tempoMarcoPontuacao / 4) % 2 == 0) {
            String plus = "+100";
            int px = 14 + hudWidth - fm.stringWidth(plus) - 10;
            g2.drawString(plus, px, y1);
        }
    }

    private void desenharTelaInicial(Graphics2D g2) {
        g2.setColor(Color.BLACK);

        g2.setFont(new Font("Arial", Font.BOLD, 42));
        desenharTextoCentralizado(g2, "JOGO DINO", 104);
        g2.drawLine(285, 122, 615, 122);

        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        desenharTextoCentralizado(g2, "Corra o maximo que conseguir sem bater.", 158);
        desenharTextoCentralizado(g2, "ESPACO / SETA CIMA  -  PULAR", 205);
        desenharTextoCentralizado(g2, "SETA BAIXO  -  AGACHAR", 232);
        desenharTextoCentralizado(g2, "P  -  PAUSAR", 259);

        g2.setFont(new Font("Arial", Font.BOLD, 20));
        if (contadorPiscaMenu < 30) {
            desenharTextoCentralizado(g2, "PRESSIONE ESPACO PARA COMECAR", 320);
        }
    }

    private void desenharGameOver(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 228));
        g2.fillRoundRect(248, 80, 404, 240, 20, 20);

        g2.setColor(Color.BLACK);
        g2.drawRoundRect(248, 80, 404, 240, 20, 20);

        g2.setFont(new Font("Arial", Font.BOLD, 30));
        desenharTextoCentralizado(g2, "GAME OVER", 110);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        desenharTextoCentralizado(g2, "Pontuacao final: " + pontuacao, 150);
        desenharTextoCentralizado(g2, "Recorde: " + melhorPontuacao, 178);

        // leaderboard display
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        int y = 200;
        List<LeaderEntry> board = new ArrayList<>();
        carregarLeaderboardEntries(board);
        for (int i = 0; i < board.size() && i < 5; i++) {
            LeaderEntry le = board.get(i);
            String s = String.format("%d. %s - %d", i + 1, le.name, le.score);
            desenharTextoCentralizado(g2, s, y + i * 18);
        }

        // name entry prompt
        if (nomeMode) {
            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            String prompt = "Digite seu nome: " + nomeBuffer + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            desenharTextoCentralizado(g2, prompt, 320);
        } else {
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            desenharTextoCentralizado(g2, "ENTER para reiniciar", 320);
        }
    }

    private void desenharPausado(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillRoundRect(300, 125, 300, 110, 20, 20);

        g2.setColor(Color.BLACK);
        g2.drawRoundRect(300, 125, 300, 110, 20, 20);

        g2.setFont(new Font("Arial", Font.BOLD, 28));
        desenharTextoCentralizado(g2, "PAUSADO", 166);

        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        desenharTextoCentralizado(g2, "Pressione P para continuar", 201);
    }

    private void desenharTextoCentralizado(Graphics2D g2, String texto, int y) {
        int larguraTexto = g2.getFontMetrics().stringWidth(texto);
        int x = (getWidth() - larguraTexto) / 2;
        g2.drawString(texto, x, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int tecla = e.getKeyCode();

        if (estado == EstadoJogo.MENU) {
            if (tecla == KeyEvent.VK_SPACE || tecla == KeyEvent.VK_ENTER) {
                iniciarJogo();
            }
            return;
        }

        if (estado == EstadoJogo.PAUSADO) {
            if (tecla == KeyEvent.VK_P) {
                estado = EstadoJogo.JOGANDO;
            }
            return;
        }

        if (estado == EstadoJogo.GAME_OVER) {
            if (nomeMode) {
                // name entry handling: backspace, chars, enter
                if (tecla == KeyEvent.VK_BACK_SPACE) {
                    if (!nomeBuffer.isEmpty()) nomeBuffer = nomeBuffer.substring(0, nomeBuffer.length() - 1);
                } else if (tecla == KeyEvent.VK_ENTER) {
                    String nameToSave = nomeBuffer.trim();
                    if (nameToSave.isEmpty()) nameToSave = "ANON";
                    try {
                        List<LeaderEntry> list = new ArrayList<>();
                        carregarLeaderboardEntries(list);
                        list.add(new LeaderEntry(nameToSave, pendingScore));
                        Collections.sort(list, (a,b) -> b.score - a.score);
                        if (list.size() > 5) list = list.subList(0, 5);
                        salvarLeaderboardEntries(list);
                    } catch (Exception ex) {}
                    nomeMode = false;
                    qualifyIndex = -1;
                    pendingScore = 0;
                } else {
                    // accept letters/numbers, limit length
                    char c = e.getKeyChar();
                    if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-') {
                        if (nomeBuffer.length() < 12) nomeBuffer += c;
                    }
                }
                return;
            } else {
                if (tecla == KeyEvent.VK_ENTER || tecla == KeyEvent.VK_SPACE) {
                    iniciarJogo();
                }
                return;
            }
        }

        if (tecla == KeyEvent.VK_P) {
            estado = EstadoJogo.PAUSADO;
            repaint();
            return;
        }

        if (tecla == KeyEvent.VK_SPACE || tecla == KeyEvent.VK_UP || tecla == KeyEvent.VK_W) {
            dino.pular();
        }

        if (tecla == KeyEvent.VK_DOWN || tecla == KeyEvent.VK_S) {
            dino.setAgachado(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
            dino.setAgachado(false);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}