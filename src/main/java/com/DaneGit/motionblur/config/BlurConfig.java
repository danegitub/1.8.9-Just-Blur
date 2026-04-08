package com.DaneGit.motionblur.config;

import java.io.*;

public class BlurConfig {

    public boolean enabled = true;
    public double strength = 0.35;

    // 0 = Basic, 1 = Faithful, 2 = Accumulation
    public int mode = 0;

    public boolean adaptive = false;
    public boolean cameraBased = false;

    public boolean halfResolution = false;
    public boolean frameSkipping = false;

    public double accumulationDecay = 0.08;

    private final File file = new File("config/motionblur.cfg");

    public void load() {
        try {
            if (!file.exists()) return;

            BufferedReader r = new BufferedReader(new FileReader(file));

            strength = Double.parseDouble(r.readLine());
            enabled = Boolean.parseBoolean(r.readLine());
            mode = Integer.parseInt(r.readLine());
            adaptive = Boolean.parseBoolean(r.readLine());
            cameraBased = Boolean.parseBoolean(r.readLine());
            halfResolution = Boolean.parseBoolean(r.readLine());
            frameSkipping = Boolean.parseBoolean(r.readLine());
            accumulationDecay = Double.parseDouble(r.readLine());

            r.close();

            strength = clamp(strength, 0.0, 1.0);
            accumulationDecay = clamp(accumulationDecay, 0.04, 0.16);

            if (mode < 0 || mode > 2) {
                mode = 0;
            }
        } catch (Exception ignored) {}
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            BufferedWriter w = new BufferedWriter(new FileWriter(file));

            w.write(String.valueOf(strength)); w.newLine();
            w.write(String.valueOf(enabled)); w.newLine();
            w.write(String.valueOf(mode)); w.newLine();
            w.write(String.valueOf(adaptive)); w.newLine();
            w.write(String.valueOf(cameraBased)); w.newLine();
            w.write(String.valueOf(halfResolution)); w.newLine();
            w.write(String.valueOf(frameSkipping)); w.newLine();
            w.write(String.valueOf(accumulationDecay));

            w.close();
        } catch (Exception ignored) {}
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}