package br.com.prog3.cannongameapp;

import android.graphics.Rect; // <--- IMPORTAÇÃO OBRIGATÓRIA

public class Target extends GameElement {
    private final int hitReward; // Recompensa de tempo

    public Target(CannonView view, int color, int hitReward, int x, int y,
                  int width, int length, float velocityY) {
        super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length, velocityY);
        this.hitReward = hitReward;
    }

    public int getHitReward() {
        return hitReward;
    }

    // --- MÉTODOS ADICIONADOS PARA CORRIGIR OS ERROS ---

    // Permite que o CannonView pegue o retângulo para checar colisão e posição
    public Rect getRect() {
        return shape; // 'shape' é herdado de GameElement
    }

    // Permite pegar a cor do alvo para fazer a explosão da mesma cor
    public int getColor() {
        return paint.getColor(); // 'paint' é herdado de GameElement
    }
}
