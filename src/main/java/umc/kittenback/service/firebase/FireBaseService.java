package umc.kittenback.service.firebase;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import umc.kittenback.converter.ImageConverter;
import umc.kittenback.converter.PostConverter;
import umc.kittenback.domain.Post;
import umc.kittenback.domain.PostImage;
import umc.kittenback.dto.image.ImageResponseDTO;
import umc.kittenback.repository.PostImageRepository;
import umc.kittenback.repository.PostRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class FireBaseService {

    @Value("${app.firebase-bucket}")
    private String firebaseBucket;

    private final PostRepository postRepository;
    private final PostImageRepository imageRepository;

    // 프로필 이미지 등록
    public String uploadProfileImage(MultipartFile file, Long userId) throws IOException {
        String fileName = generateProfileFileName(file.getOriginalFilename(), userId);
        Bucket bucket = StorageClient.getInstance().bucket(firebaseBucket);
        Blob blob = bucket.create(fileName, file.getInputStream(), file.getContentType());
        String mediaLink = blob.getMediaLink();
        return mediaLink;
    }

    // 펫 이미지 등록
    public String uploadPetImage(MultipartFile file, Long petId) throws IOException {
        String fileName = generatePetFileName(file.getOriginalFilename(), petId);
        Bucket bucket = StorageClient.getInstance().bucket(firebaseBucket);
        Blob blob = bucket.create(fileName, file.getInputStream(), file.getContentType());
        String mediaLink = blob.getMediaLink();
        return mediaLink;
    }

    // 게시판 img file 등록
    public String uploadFile(MultipartFile file, Long userId) throws IOException {
        Bucket bucket = StorageClient.getInstance().bucket(firebaseBucket);
        InputStream content = new ByteArrayInputStream(file.getBytes());
//        Blob blob = bucket.create(fileName.toString(), content, file.getContentType());
//        String storagePath = "posts/" + fileName.toString();
        String fileName = generatePostFileName(file.getOriginalFilename(), userId);
        Blob blob = bucket.create(fileName, content, file.getContentType());
        return blob.getMediaLink();
    }

    // 게시판 img files 등록
    public List<String> uploadFiles(List<MultipartFile> files, Long postId) throws IOException {
        Post post = postRepository.findById(postId).get();

        List<String> mediaLinks = new ArrayList<>();
        Bucket bucket = StorageClient.getInstance().bucket(firebaseBucket);

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileName = generatePostFileName(file.getOriginalFilename(), postId);
                InputStream content = new ByteArrayInputStream(file.getBytes());
                Blob blob = bucket.create(fileName, content, file.getContentType());
                mediaLinks.add(blob.getMediaLink());
                System.out.println("1");
                imageRepository.save(ImageConverter.toPostImage(post, blob.getMediaLink()));
                System.out.println("2");
            }

        }
        return mediaLinks;
    }

    // (게시글)파일 이름 생성
    public String generatePostFileName(String originalFilename, Long postId) {
        return "posts/" + postId + "/" + originalFilename;
    }

    // (프로필) 파일 이름 생성
    public String generateProfileFileName(String originalFilename, Long userId) {
        return "profile/" + userId + "/" + originalFilename;
    }

    // 펫 파일 이름 생성
    public String generatePetFileName(String originalFilename, Long petId) {
        return "pet/" + petId + "/" + originalFilename;
    }

    // (게시글) 파일 불러오기
    public byte[] getFile(Long userId) {
        Bucket bucket = StorageClient.getInstance().bucket(firebaseBucket);
        return bucket.get("posts/" + userId).getContent();
    }

    // (게시글) 파일들 불러오기
    public List<Blob> getFiles(Long userId) {
        Bucket bucket = StorageClient.getInstance().bucket(firebaseBucket);
        String prefix = "posts/" + userId;
        // Blob 목록 가져옴(Blob 객체의 Iterable을 반환)
        // prefix로 시작하는 blob 반환
        Iterable<Blob> blobs = bucket.list(Storage.BlobListOption.prefix(prefix)).iterateAll();
        List<Blob> fileList = new ArrayList<>();
        blobs.forEach(fileList::add);
        return fileList;
    }
}
