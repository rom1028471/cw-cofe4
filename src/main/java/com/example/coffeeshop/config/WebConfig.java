package com.example.coffeeshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.product-image-path:src/main/resources/static/media/products/}")
    private String productImageUploadPathStr;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Убедимся, что путь заканчивается на / для правильного сопоставления
        String uploadPath = productImageUploadPathStr;
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }

        // Обслуживать файлы из /media/products/** по пути file:.../src/main/resources/static/media/products/
        // Это особенно полезно для разработки, чтобы видеть изменения без пересборки.
        registry.addResourceHandler("/media/products/**")
                .addResourceLocations("file:" + uploadPath);
        
        // Можно оставить и стандартный обработчик для других статических ресурсов, 
        // если он не конфликтует или если у вас есть другие ресурсы в classpath:/static/
        // registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }
} 