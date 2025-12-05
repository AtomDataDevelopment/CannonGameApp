package br.com.prog3.cannongameapp;

import android.graphics.Rect; // <--- NÃO ESQUEÇA DESTA IMPORTAÇÃO

public class Blocker extends GameElement {
    private int missPenalty; // Penalidade de tempo

    public Blocker(CannonView view, int color, int missPenalty, int x,
                   int y, int width, int length, float velocityY) {
        super(view, color, CannonView.BLOCKER_SOUND_ID, x, y, width, length, velocityY);
        this.missPenalty = missPenalty;
    }

    public int getMissPenalty() {
        return missPenalty;
    }

    // --- ADICIONE ESTE MÉTODO PARA CORRIGIR O ERRO ---
    public Rect getRect() {
        return shape; // 'shape' é a variável herdada de GameElement
    }
}
