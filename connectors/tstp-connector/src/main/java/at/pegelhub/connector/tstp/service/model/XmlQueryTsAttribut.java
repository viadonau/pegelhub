package at.pegelhub.connector.tstp.service.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@Getter @Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlQueryTsAttribut {
    @XmlElement(name = "ZRID")
    private String zrid;
    @XmlElement(name = "MAXFOCUS-Start")
    private String maxFocusStart;
    @XmlElement(name = "MAXFOCUS-End")
    private String maxFocusEnd;
    @XmlElement(name = "MAXQUAL")
    private String maxQual;
    @XmlElement(name = "PARAMETER")
    private String parameter;
    @XmlElement(name = "ORT")
    private String ort;
    @XmlElement(name = "DEFART")
    private String defArt;
    @XmlElement(name = "AUSSAGE")
    private String aussage;
    @XmlElement(name = "XDISTANZ")
    private String xDistanz;
    @XmlElement(name = "XFAKTOR")
    private String xFaktor;
    @XmlElement(name = "HERKUNFT")
    private String herkunft;
    @XmlElement(name = "REIHENART")
    private String reihenArt;
    @XmlElement(name = "VERSION")
    private String version;
    @XmlElement(name = "X")
    private String x;
    @XmlElement(name = "Y")
    private String y;
    @XmlElement(name = "GUELTVON")
    private String gueltVon;
    @XmlElement(name = "GUELTBIS")
    private String gueltBis;
    @XmlElement(name = "EINHEIT")
    private String einheit;
    @XmlElement(name = "MESSGENAU")
    private String messGenau;
    @XmlElement(name = "FTOLERANZ")
    private String fToleranz;
    @XmlElement(name = "FTOLREL")
    private String fTolRel;
    @XmlElement(name = "NWGRENZE")
    private String nwGrenze;
    @XmlElement(name = "SUBORT")
    private String subOrt;
    @XmlElement(name = "KOMMENTAR")
    private String kommentar;
    @XmlElement(name = "HOEHE")
    private String hoehe;
    @XmlElement(name = "YTYP")
    private String yTyp;
    @XmlElement(name = "XEINHEIT")
    private String xEinheit;
    @XmlElement(name = "QUELLE")
    private String quelle;
    @XmlElement(name = "PUBLIZIERT")
    private String publiziert;
    @XmlElement(name = "PARMERKMAL")
    private String parMerkmal;
    @XmlElement(name = "HAUPTREIHE")
    private String hauptReihe;
    @XmlElement(name = "MAXTEXTFOCUS-Start")
    private String maxTextFocusStart;
    @XmlElement(name = "MAXTEXTFOCUS-End")
    private String maxTextFocusEnd;

}
