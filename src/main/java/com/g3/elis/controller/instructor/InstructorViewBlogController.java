package com.g3.elis.controller.instructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.g3.elis.config.FileStorageConfig;
import com.g3.elis.dto.form.BlogPostDto;
import com.g3.elis.model.BlogPost;
import com.g3.elis.service.BlogPostService;
import com.g3.elis.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/instructor")
public class InstructorViewBlogController {

    private final String HTML_FILE_PATH = "src/main/resources/static/private/blog/blog-files/";
    private final String IMAGE_FILE_PATH = "src/main/resources/static/private/blog/blog-images";

    @Autowired
    private BlogPostService blogPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageConfig fileStorageConfig;

    // GET Mapping to view all blogs
    @GetMapping("/instructor-view-blog")
    public String instructorViewBlog(Model model) {
        List<BlogPost> blogPosts = blogPostService.getAllBlogPosts();
        model.addAttribute("blogPosts", blogPosts);
        model.addAttribute("content", "instructor/instructor-view-blog");
        return "/instructor/instructor-layout";
    }

    // GET Mapping to view blog details
    @GetMapping("/blog-detail/{id}")
    public String instructorViewBlogDetail(@PathVariable("id") int id, Model model) throws IOException {
        BlogPost blogPost = blogPostService.findById(id);

        if (blogPost != null) {
            LocalDateTime createdAt = blogPost.getCreatedAt().toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(createdAt, now);
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            String timeAgo = String.format("%d days, %d hours, %d minutes ago", days, hours, minutes);

            model.addAttribute("timeAgo", timeAgo);

            // Read HTML file content
            java.nio.file.Path htmlFilePath = Paths.get(HTML_FILE_PATH + blogPost.getHtmlFileName());
            String htmlContent = Files.readString(htmlFilePath);
            model.addAttribute("htmlContent", htmlContent);
            model.addAttribute("blogPost", blogPost);
            return "/authenticated-user/blog-detail";
        }

        return "redirect:/admin/admin-view-blog";
    }

    // GET Mapping to return the HTML content of a blog file
    @GetMapping("/blog-html/{fileName}")
    @ResponseBody
    public String getBlogHtml(@PathVariable String fileName) throws IOException {
        java.nio.file.Path htmlFilePath = Paths.get(HTML_FILE_PATH + fileName);
        return Files.readString(htmlFilePath);
    }

    // POST Mapping to handle blog creation or update
    @PostMapping("/instructor-view-blog")
    public String saveBlogPost(@Valid BlogPostDto blogPostDto, MultipartFile imgFile, String content) {
        try {
            // Handle image file upload
            if (!imgFile.isEmpty()) {
                String originalFileName = imgFile.getOriginalFilename();
                String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
                String newImageFileName = dateTime + "_" + originalFileName;
                String imagePath = IMAGE_FILE_PATH + newImageFileName;

                Files.write(Paths.get(imagePath), imgFile.getBytes());
                blogPostDto.setImageFile(newImageFileName);
            }

            // Handle HTML file upload
            BlogPost blogPost;
            if (blogPostDto.getId() > 0) {
                blogPost = blogPostService.findById(blogPostDto.getId());
                if (blogPost == null) {
                    return "redirect:/edit/{id}";
                }
            } else {
                blogPost = new BlogPost();
                String fileName = UUID.randomUUID().toString() + ".html";
                blogPostDto.setHtmlFileName(fileName);
            }

            String filePath = HTML_FILE_PATH + blogPostDto.getHtmlFileName();
            if (Files.exists(Paths.get(filePath))) {
                blogPostDto.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                blogPostDto.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                blogPostDto.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                blogPostDto.setUsers(userService.getCurrentUser());
                Files.write(Paths.get(filePath), content.getBytes());
            }

            blogPostService.saveBlogPost(blogPostDto);
            return "redirect:/instructor/instructor-view-blog";

        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/instructor/instructor-view-blog";
        }
    }
}
