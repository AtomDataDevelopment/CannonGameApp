package br.com.prog3.cannongameapp;

import android.graphics.Rect; // <--- IMPORTAÇÃO NECESSÁRIA

public class Blocker extends GameElement {
    private final int missPenalty; // Penalidade de tempo

    public Blocker(CannonView view, int color, int missPenalty, int x,
                   int y, int width, int length, float velocityY) {
        super(view, color, CannonView.BLOCKER_SOUND_ID, x, y, width, length, velocityY);
        this.missPenalty = missPenalty;
    }

    public int getMissPenalty() {
        return missPenalty;
    }

    // --- MÉTODO ADICIONADO PARA CORRIGIR O ERRO ---
    public Rect getRect() {
        return shape; // Retorna a forma herdada de GameElement
    }
}
