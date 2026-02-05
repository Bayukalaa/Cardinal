package dev.onlydarkness.utils.ai;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;

public class AIService {
    private static AIService instance;
    private MultiLayerNetwork model;
    private DataNormalization dataNormalization;
    private boolean isReady = false;

    private AIService() {
        try {
            System.out.println("🧠 Cardinal AI yükleniyor...");
            File modelFile = new File("cardinal_brain.zip");
            File normFile = new File("cardinal_normalizer.zip");
            if (modelFile.exists() && normFile.exists()) {
                this.model = MultiLayerNetwork.load(modelFile, true);
                this.dataNormalization = NormalizerSerializer.getDefault().restore(normFile);
                this.isReady = true;
                System.out.println("✅ AI başarıyla devreye girdi!");
            } else {
                System.err.println("❌ Model dosyaları bulunamadı! Lütfen önce eğitimi tamamla.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }

    public int analyzeSystem(double aktifKullanici, double ram, double cpu, double disk, double network) {
        if (!isReady) return 0;


        double[] data = {
                aktifKullanici,
                aktifKullanici * 1.5,
                5,
                0.5,
                0.5,
                ram,
                cpu,
                disk,
                network
        };


        float[] floatData = new float[data.length];
        for (int i = 0; i < data.length; i++) floatData[i] = (float) data[i];

        INDArray input = Nd4j.create(new float[][]{floatData});


        dataNormalization.transform(input);
        return model.output(input).argMax(1).getInt(0);
    }
}
