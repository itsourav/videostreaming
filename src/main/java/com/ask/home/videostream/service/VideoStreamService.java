package com.ask.home.videostream.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import static com.ask.home.videostream.constants.ApplicationConstants.*;

@Service
public class VideoStreamService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AmazonS3 amazonS3Client;

    @Value("${aws.services.bucket}")
    private String bucketName;

    public int BUFFER_SIZE = 355856562;

    /**
     * Prepare the content.
     *
     * @param fileName String.
     * @param fileType String.
     * @param range    String.
     * @return ResponseEntity.
     */
    public ResponseEntity<byte[]> prepareContent(String fileName, String fileType, String range) {
        long rangeStart = 0;
        long rangeEnd;
        byte[] data;
        Long fileSize;
        String fullFileName = fileName + "." + fileType;
        try {
            if(fileName.equals("toystory")){
                fileSize =33505479L;
                BUFFER_SIZE = 33505479;
            }
            else{
                fileSize = 355856562L;//getFileSize(fullFileName);
                BUFFER_SIZE = 355856562;
            }
           
                       
            logger.info("File Size :: {}",fileSize);
            if (range == null) {
                return ResponseEntity.status(HttpStatus.OK)
                        .header(CONTENT_TYPE, VIDEO_CONTENT + fileType)
                        .header(CONTENT_LENGTH, String.valueOf(fileSize - 1))
                        .body(readS3ByteRange(fullFileName, rangeStart, fileSize)); // Read the object and convert it as bytes
            }
            String[] ranges = range.split("-");
            rangeStart = Long.parseLong(ranges[0].substring(6));
            if (ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            } else {
                rangeEnd = fileSize;
            }
            if (fileSize < rangeEnd) {
                rangeEnd = fileSize;
            }
            data = readS3ByteRange(fullFileName, rangeStart, rangeEnd);
        } catch (IOException e) {
            logger.error("Exception while reading the file {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(CONTENT_TYPE, VIDEO_CONTENT + fileType)
                .header(ACCEPT_RANGES, BYTES)
                .header(CONTENT_LENGTH, contentLength)
                .header(CONTENT_RANGE, BYTES + " " + rangeStart + "-" + (rangeEnd - 1) + "/" + fileSize)
                .body(data);


    }

    /**
     * ready file byte by byte.
     *
     * @param filename String.
     * @param start    long.
     * @param end      long.
     * @return byte array.
     * @throws IOException exception.
     */
/*    public byte[] readByteRange(String filename, long start, long end) throws IOException {
        Path path = Paths.get(getFilePath(), filename);
        try (InputStream inputStream = (Files.newInputStream(path));
             ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream()) {
            byte[] data = new byte[BYTE_RANGE];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                bufferedOutputStream.write(data, 0, nRead);
            }
            bufferedOutputStream.flush();
            byte[] result = new byte[(int) (end - start)];
            System.arraycopy(bufferedOutputStream.toByteArray(), (int) start, result, 0, (int) (end - start));
            return result;
        }
    }*/

    public byte[] readS3ByteRange(String filename, long start, long end) throws IOException {
      
        byte[] bytes = new byte[BUFFER_SIZE];;
        try {
            final S3Object s3Object = amazonS3Client.getObject(new GetObjectRequest(bucketName, filename).withRange(start,end));
            final S3ObjectInputStream inputStream = s3Object.getObjectContent();
           
            bytes = StreamUtils.copyToByteArray(inputStream);


                return bytes;
        }catch (AmazonS3Exception amazonS3Exception){
            throw amazonS3Exception;
        }
      
    }

    /**
     * Get the filePath.
     *
     * @return String.
     */
    private String getFilePath() {
        String path = "/Users/sourav/Documents/Java Projects/video";
        URL url = this.getClass().getResource(path);
       // return new File(url.getFile()).getAbsolutePath();
       return path;
    }
    
    /**
     * Content length.
     *
     * @param fileName String.
     * @return Long.
     */
    public Long getFileSize(String fileName) {
        return Optional.ofNullable(fileName)
                .map(file -> Paths.get(getFilePath(), file))
                .map(this::sizeFromFile)
                .orElse(0L);
    }

    /**
     * Getting the size from the path.
     *
     * @param path Path.
     * @return Long.
     */
    private Long sizeFromFile(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ioException) {
            logger.error("Error while getting the file size", ioException);
        }
        return 0L;
    }
}