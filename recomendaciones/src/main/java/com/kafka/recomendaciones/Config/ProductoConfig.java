package com.kafka.recomendaciones.Config;

import org.springframework.stereotype.Component;

import com.kafka.recomendaciones.Model.Producto;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class ProductoConfig {

    // Catálogo maestro en memoria con múltiples productos
    private final List<Producto> productos = Arrays.asList(
        // Electrónica
        new Producto("item_101", "Audífonos Inalámbricos Pro", "electronica", "audio", "Sony", 89990, 4.8, "https://example.com/img/sony.jpg", 25),
        new Producto("item_102", "Teclado Mecánico RGB Gamer", "electronica", "perifericos", "Redragon", 45990, 4.6, "https://example.com/img/keyboard.jpg", 40),
        new Producto("item_107", "Smartwatch Deportivo Fit", "electronica", "wearables", "Xiaomi", 29990, 4.5, "https://example.com/img/xiaomi.jpg", 60),
        new Producto("item_109", "Monitor 24\" IPS 144Hz", "electronica", "monitores", "LG", 179900, 4.9, "https://example.com/img/monitor.jpg", 12),
        new Producto("item_110", "Mouse Inalámbrico Ergonómico", "electronica", "perifericos", "Logitech", 24990, 4.7, "https://example.com/img/mouse.jpg", 35),
        new Producto("item_111", "Parlante Bluetooth Resistente al Agua", "electronica", "audio", "JBL", 59990, 4.8, "https://example.com/img/jbl.jpg", 22),
        new Producto("item_112", "Notebook Gamer 15.6\"", "electronica", "computacion", "ASUS", 799900, 4.6, "https://example.com/img/asus.jpg", 5),

        // Ropa y Calzado
        new Producto("item_103", "Zapatillas Running Air", "ropa", "calzado", "Nike", 69990, 4.7, "https://example.com/img/nike.jpg", 15),
        new Producto("item_104", "Polerón con Cierre Sport", "ropa", "vestuario", "Adidas", 39990, 4.4, "https://example.com/img/adidas.jpg", 30),
        new Producto("item_113", "Chaqueta Impermeable Trekking", "ropa", "vestuario", "The North Face", 89990, 4.9, "https://example.com/img/tnf.jpg", 18),
        new Producto("item_114", "Jeans Slim Fit Clásicos", "ropa", "vestuario", "Levi's", 42990, 4.3, "https://example.com/img/levis.jpg", 25),
        new Producto("item_115", "Gorro Deportivo Dri-FIT", "ropa", "accesorios", "Nike", 14990, 4.6, "https://example.com/img/gorro.jpg", 50),

        // Hogar y Cocina
        new Producto("item_105", "Cafetera Italiana Express", "hogar", "cocina", "DeLonghi", 55990, 4.9, "https://example.com/img/coffee.jpg", 10),
        new Producto("item_106", "Mochila Antirrobo Portátil", "hogar", "accesorios", "XD Design", 34990, 4.3, "https://example.com/img/backpack.jpg", 50),
        new Producto("item_116", "Aspiradora Robot Inteligente", "hogar", "electrodomesticos", "Roborock", 249900, 4.8, "https://example.com/img/robot.jpg", 8),
        new Producto("item_117", "Set de Sartenes Antiadherentes", "hogar", "cocina", "T-fal", 39990, 4.5, "https://example.com/img/sarten.jpg", 14),

        // Deportes y Bienestar
        new Producto("item_108", "Proteína Whey 1kg", "deportes", "suplementos", "Optimum Nutrition", 49990, 4.9, "https://example.com/img/protein.jpg", 20),
        new Producto("item_118", "Mancuernas Ajustables 20kg", "deportes", "fitness", "ProForm", 99990, 4.7, "https://example.com/img/mancuernas.jpg", 7),
        new Producto("item_119", "Bicicleta de Spinning Indoor", "deportes", "ciclismo", "Oxford", 349900, 4.4, "https://example.com/img/bike.jpg", 4),
        new Producto("item_120", "Mat de Yoga Antideslizante", "deportes", "fitness", "Manduka", 21990, 4.8, "https://example.com/img/mat.jpg", 45)
    );

    public List<Producto> obtenerTodos() {
        return productos;
    }

    public Optional<Producto> buscarPorId(String id) {
        return productos.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }
}