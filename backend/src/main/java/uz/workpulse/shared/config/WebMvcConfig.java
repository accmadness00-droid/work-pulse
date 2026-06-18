package uz.workpulse.shared.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uz.workpulse.shared.file.FileStorageProperties;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.uploads.photo-dir:uploads/photos}")
    private String photoDir;

    private final FileStorageProperties fileStorageProperties;

    public WebMvcConfig(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/photos/**")
                .addResourceLocations(fileLocation(photoDir));
        registry.addResourceHandler(fileStorageProperties.getPublicBaseUrl().replaceAll("/$", "") + "/**")
                .addResourceLocations(fileLocation(fileStorageProperties.getUploadDir()));
    }

    private String fileLocation(String directory) {
        return Path.of(directory).toAbsolutePath().normalize().toUri().toString();
    }
}
