package at.pegelhub.quality.api;

import at.pegelhub.quality.application.Quality;
import at.pegelhub.quality.application.QualityProperties;
import at.pegelhub.quality.domain.Finding;
import at.pegelhub.quality.domain.ProfileConfig;
import at.pegelhub.quality.domain.QualityProfile;
import at.pegelhub.quality.domain.QualityRun;
import at.pegelhub.shared.api.ConfigurationYaml;
import at.pegelhub.shared.api.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** HTTP boundary; SecurityConfiguration distinguishes administrator configuration from monitoring result access. */
@RestController
@RequestMapping("/api/v1/quality")
public class QualityController {

    private final Quality quality;
    private final QualityProperties properties;
    private final ConfigurationYaml yaml;

    public QualityController(Quality quality, QualityProperties properties, ConfigurationYaml yaml) {
        this.quality = quality;
        this.properties = properties;
        this.yaml = yaml;
    }

    @GetMapping("/status")
    public QualityProperties status() {
        return properties;
    }

    @GetMapping("/profiles")
    public List<QualityProfile> profiles() {
        return quality.profiles();
    }

    @GetMapping("/profiles/{id}")
    public QualityProfile profile(@PathVariable UUID id) {
        return quality.profile(id);
    }

    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public QualityProfile create(@RequestBody ProfileConfig config) {
        return quality.create(config);
    }

    @PutMapping("/profiles/{id}")
    public QualityProfile update(@PathVariable UUID id, @RequestBody ProfileConfig config) {
        return quality.update(id, config);
    }

    @PostMapping("/profiles/{id}/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestRun(@PathVariable UUID id) {
        quality.requestRun(id);
    }

    @GetMapping("/runs")
    public Page<QualityRun> runs(
            @RequestParam(required = false) UUID profileId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return quality.runs(profileId, offset, limit);
    }

    @GetMapping("/runs/{id}")
    public QualityRun run(@PathVariable UUID id) {
        return quality.run(id);
    }

    @GetMapping("/runs/{id}/findings")
    public Page<Finding> findings(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return quality.findings(id, offset, limit);
    }

    @GetMapping(value = "/profiles/{id}/export", produces = "application/yaml")
    public String export(@PathVariable UUID id) {
        return yaml.write(quality.profile(id).configuration());
    }

    @PostMapping(value = "/profiles/import", consumes = {"application/yaml", "text/plain"})
    @ResponseStatus(HttpStatus.CREATED)
    public QualityProfile importConfig(@RequestBody String document) {
        return quality.create(yaml.read(document, ProfileConfig.class));
    }
}
