package com.DaneGit.motionblur.config;

import java.io.*;

public class BlurConfig {

    public boolean enabled = true;
    public double strength = 0.35;

    public boolean useFaithful = false;
    public boolean adaptive = false;

    private final File file = new File("config/motionblur.cfg");

    public void load() {
        try {
            if (!file.exists()) return;

            BufferedReader reader = new BufferedReader(new FileReader(file));

            String s1 = reader.readLine();
            String s2 = reader.readLine();
            String s3 = reader.readLine();
            String s4 = reader.readLine();

            reader.close();

            if (s1 != null) strength = Double.parseDouble(s1);
            if (s2 != null) enabled = Boolean.parseBoolean(s2);
            if (s3 != null) useFaithful = Boolean.parseBoolean(s3);
            if (s4 != null) adaptive = Boolean.parseBoolean(s4);

        } catch (Exception ignored) {}
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            writer.write(String.valueOf(strength)); writer.newLine();
            writer.write(String.valueOf(enabled)); writer.newLine();
            writer.write(String.valueOf(useFaithful)); writer.newLine();
            writer.write(String.valueOf(adaptive));

            writer.close();

        } catch (Exception ignored) {}
    }
}