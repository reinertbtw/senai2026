public class GameConfig {

    public static final int LARGURA_TELA = 900;
    public static final int ALTURA_TELA = 400;

    public static final int CHAO_Y = 310;

    public static final int DINO_X = 80;
    public static final int DINO_LARGURA = 40;
    public static final int DINO_ALTURA = 50;

    public static final double GRAVIDADE = 1.0;
    public static final double FORCA_PULO = -14.0;

    public static final int FPS_DELAY = 20;

    // Power-up and gameplay tuning
    // Number of ticks (FPS_DELAY ms each) that a power-up lasts (e.g., 6s -> 6*1000/20 = 300 ticks)
    public static final int POWERUP_DURATION_TICKS = 300;

    // Chance per tick to spawn a power-up (small)
    // Spawn chance (tuned lower). Increase for testing if needed.
    public static final double POWERUP_SPAWN_CHANCE = 0.002;

    // Minimum spawn interval for obstacles (ticks)
    public static final int MIN_INTERVALO_SPAWN = 40;
}