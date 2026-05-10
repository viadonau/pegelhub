package at.pegelhub.connector.tstp.service.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Getter @Setter
@XmlRootElement(name = "TSQ")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlQueryResponse {
    @XmlElement(name = "TSATTR")
    private List<XmlQueryTsAttribut> def;
}
