package com.customfield.util;

import com.customfield.model.CustomField;
import com.customfield.model.FieldRelation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class FileStorageUtil {

    @Value("${app.data-dir}")
    private String dataDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private File customFieldFile;
    private File fieldRelationFile;

    @PostConstruct
    public void init() {
        // Resolve absolute path
        Path dirPath = Paths.get(dataDir).toAbsolutePath().normalize();
        
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create data directory", e);
        }

        customFieldFile = dirPath.resolve("custom-field.json").toFile();
        fieldRelationFile = dirPath.resolve("field-relation.json").toFile();

        // Initialize files if they don't exist
        try {
            if (!customFieldFile.exists()) {
                objectMapper.writeValue(customFieldFile, new ArrayList<CustomField>());
            }
            if (!fieldRelationFile.exists()) {
                objectMapper.writeValue(fieldRelationFile, new FieldRelation());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize data files", e);
        }
    }

    public List<CustomField> readCustomFields() {
        lock.readLock().lock();
        try {
            if (!customFieldFile.exists()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(customFieldFile, new TypeReference<List<CustomField>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read custom fields", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void writeCustomFields(List<CustomField> fields) {
        lock.writeLock().lock();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(customFieldFile, fields);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write custom fields", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public FieldRelation readFieldRelation() {
        lock.readLock().lock();
        try {
            if (!fieldRelationFile.exists()) {
                return new FieldRelation();
            }
            return objectMapper.readValue(fieldRelationFile, FieldRelation.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read field relation", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void writeFieldRelation(FieldRelation relation) {
        lock.writeLock().lock();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fieldRelationFile, relation);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write field relation", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
