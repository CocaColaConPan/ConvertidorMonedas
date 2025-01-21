package com.version1.convertidor;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ConvertidorMonedas {
    private static final Properties properties = new Properties();
    private static String API_KEY;
    private static String BASE_URL;
    private static List<Moneda> monedasDisponibles;

    static {
        try (InputStream input = ConvertidorMonedas.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("No se pudo encontrar config.properties");
            }
            properties.load(input);
            API_KEY = properties.getProperty("api.key");
            BASE_URL = properties.getProperty("api.base.url") + API_KEY + "/latest/";
            monedasDisponibles = obtenerMonedasDisponibles();
        } catch (IOException ex) {
            throw new RuntimeException("Error al cargar la configuración", ex);
        }
    }

    public static void main(String[] args) {
        // Ventana
        JFrame frame = new JFrame("Convertidor de Monedas");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new GridLayout(4, 2));

        // Campos
        JLabel labelMonedaBase = new JLabel("Moneda Base:");
        JComboBox<Moneda> comboMonedaBase = new JComboBox<>(monedasDisponibles.toArray(new Moneda[0]));
        JLabel labelMonedaDestino = new JLabel("Moneda Destino:");
        JComboBox<Moneda> comboMonedaDestino = new JComboBox<>(monedasDisponibles.toArray(new Moneda[0]));
        JLabel labelCantidad = new JLabel("Cantidad:");
        JTextField textCantidad = new JTextField();
        JButton buttonConvertir = new JButton("Convertir");
        JLabel labelResultado = new JLabel("");

        // Labels de los campos en la ventana
        frame.add(labelMonedaBase);
        frame.add(comboMonedaBase);
        frame.add(labelMonedaDestino);
        frame.add(comboMonedaDestino);
        frame.add(labelCantidad);
        frame.add(textCantidad);
        frame.add(buttonConvertir);
        frame.add(labelResultado);

        // Acción del botón
        buttonConvertir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Moneda monedaBase = (Moneda) comboMonedaBase.getSelectedItem();
                Moneda monedaDestino = (Moneda) comboMonedaDestino.getSelectedItem();
                double cantidad = Double.parseDouble(textCantidad.getText());

                try {
                    double resultado = convertirMoneda(monedaBase.getCodigo(), monedaDestino.getCodigo(), cantidad);
                    labelResultado.setText(String.format("%.2f %s son %.2f %s", cantidad, monedaBase.getCodigo(), resultado, monedaDestino.getCodigo()));
                } catch (IOException ex) {
                    labelResultado.setText("Error al convertir la moneda: " + ex.getMessage());
                }
            }
        });

        // Ventana visible
        frame.setVisible(true);
    }

    public static double convertirMoneda(String monedaBase, String monedaDestino, double cantidad) 
            throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + monedaBase;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error en la respuesta: " + response);
            }

            String jsonData = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonData);
            double tasaCambio = jsonObject.getJSONObject("conversion_rates").getDouble(monedaDestino);
            return cantidad * tasaCambio;
        }
    }

    private static List<Moneda> obtenerMonedasDisponibles() throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/codes"; // Llamada a la API para obtener los códigos de moneda

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error en la respuesta: " + response);
            }

            String jsonData = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray supportedCodes = jsonObject.getJSONArray("supported_codes");

            List<Moneda> monedas = new ArrayList<>();
            for (int i = 0; i < supportedCodes.length(); i++) {
                JSONArray monedaArray = supportedCodes.getJSONArray(i);
                String codigo = monedaArray.getString(0);
                String nombre = monedaArray.getString(1);
                monedas.add(new Moneda(codigo, nombre));
            }
            return monedas;
        }
    }

    // Clase para representar una moneda
    static class Moneda {
        private String codigo;
        private String nombre;

        public Moneda(String codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return codigo + " (" + nombre + ")";
        }
    }
}