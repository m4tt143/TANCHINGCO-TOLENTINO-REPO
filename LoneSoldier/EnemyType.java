public enum EnemyType {
    ZOMBIE(1f, 1f, false, false, false),
    RUNNER(0.7f, 1.6f, false, false, false),
    TANK(2.8f, 0.55f, false, false, false),
    RANGED(0.9f, 0.85f, true, false, false),
    EXPLODER(0.8f, 1.3f, false, true, false),
    BOSS(12f, 0.45f, true, false, true);

    public final float hpMult;
    public final float speedMult;
    public final boolean canShoot;
    public final boolean explodes;
    public final boolean isBoss;

    EnemyType(float hpMult, float speedMult, boolean canShoot, boolean explodes, boolean isBoss) {
        this.hpMult = hpMult;
        this.speedMult = speedMult;
        this.canShoot = canShoot;
        this.explodes = explodes;
        this.isBoss = isBoss;
    }
}