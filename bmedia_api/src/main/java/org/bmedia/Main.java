package org.bmedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @RequestMapping(value = "/", produces = "text/plain")
    public ResponseEntity<String> defaultResponse() {
        return ResponseEntity.status(HttpStatus.OK).body("This is the BMedia API");
    }

    @RequestMapping(value = "/search_images/by_tag/all", produces = "application/json")
    public ResponseEntity<String> search_images_by_tag_all(@RequestParam("tags") String[] tags) {


        return ResponseEntity.status(HttpStatus.OK).body("This is the BMedia API");
    }

    @RequestMapping(value = "/search_images/by_tag/all", produces = "application/json")
    public ResponseEntity<String> search_images_by_tag_page(@RequestParam("tags") String[] tags,
                                                            @RequestParam("page_num") int pageNum,
                                                            @RequestParam("results_per_page") int resultsPerPage) {
        return ResponseEntity.status(HttpStatus.OK).body("This is the BMedia API");
    }
}