package com.sistema.base.api.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path fileStoragePath;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_IMAGE_SIZE = 200; // 200x200 pixels
    private static final float JPEG_QUALITY = 0.8f; // 80% calidad

    @PostConstruct
    public void init() {
        this.fileStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de almacenamiento: " + uploadDir, e);
        }
    }

    public String storeFile(MultipartFile file, String userId) throws IOException {
        // Validar que el archivo no esté vacío
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        // Validar tamaño del archivo
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 5MB");
        }

        // Obtener y validar extensión del archivo
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo se permiten: " + 
                String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Comprimir y redimensionar la imagen
        InputStream processedImageStream = compressImage(file.getInputStream(), fileExtension);

        // Generar nombre único para el archivo (siempre guardamos como jpg después de compresión)
        String outputExtension = fileExtension.equals("png") || fileExtension.equals("gif") ? fileExtension : "jpg";
        String newFilename = "profile_" + userId + "_" + UUID.randomUUID().toString() + "." + outputExtension;

        // Guardar el archivo comprimido
        Path targetLocation = this.fileStoragePath.resolve(newFilename);
        Files.copy(processedImageStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Retornar la ruta relativa para almacenar en la base de datos
        return "/uploads/profile-images/" + newFilename;
    }

    /**
     * Comprime y redimensiona una imagen a máximo 200x200px con calidad JPEG 80%
     */
    private InputStream compressImage(InputStream inputStream, String extension) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        
        if (originalImage == null) {
            throw new IllegalArgumentException("No se pudo leer la imagen");
        }

        // Calcular nuevas dimensiones manteniendo proporción
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        if (width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE) {
            double scale = Math.min((double) MAX_IMAGE_SIZE / width, (double) MAX_IMAGE_SIZE / height);
            width = (int) (width * scale);
            height = (int) (height * scale);
        }

        // Redimensionar imagen
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, width, height, null);
        graphics.dispose();

        // Comprimir y convertir a bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        if (extension.equals("png") || extension.equals("gif")) {
            // PNG y GIF no soportan compresión JPEG, los guardamos tal cual
            ImageIO.write(resizedImage, extension, outputStream);
        } else {
            // Comprimir como JPEG con calidad 80%
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IllegalStateException("No hay escritores JPEG disponibles");
            }
            
            ImageWriter writer = writers.next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(ios);
            
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(JPEG_QUALITY);
            
            writer.write(null, new IIOImage(resizedImage, null, null), params);
            writer.dispose();
            ios.close();
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        try {
            // Extraer solo el nombre del archivo de la ruta
            String filename = filePath.substring(filePath.lastIndexOf('/') + 1);
            Path fileToDelete = this.fileStoragePath.resolve(filename);
            Files.deleteIfExists(fileToDelete);
        } catch (IOException e) {
            // Log del error pero no lanzar excepción para no interrumpir el flujo
            System.err.println("Error al eliminar archivo: " + filePath + " - " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }


}

