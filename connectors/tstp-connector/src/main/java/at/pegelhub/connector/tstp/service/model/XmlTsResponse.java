package at.pegelhub.connector.tstp.service.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.*;

@Getter @Setter
@XmlRootElement(name = "TSR")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlTsResponse {
    @XmlAttribute(name = "RELEASE")
    private String release;
    @XmlValue
    private String message;
}
