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

    public XmlQueryTsAttribut() {
        this.zrid = "PK8n4XrPPUfYpndH6GLH6A";
        this.maxFocusStart = "1960-05-06T12:02:00Z";
        this.maxFocusEnd = "2001-08-01T00:00:00Z";
        this.maxQual = "1";
        this.parameter = "Wasserstand";
        this.ort = "24004501";
        this.defArt = "K";
        this.aussage = "";
        this.xDistanz = "E";
        this.xFaktor = "1";
        this.herkunft = "O";
        this.reihenArt = "Z";
        this.version = "0";
        this.x = "2526320";
        this.y = "5640320";
        this.gueltVon = "";
        this.gueltBis = "";
        this.einheit = "cm";
        this.messGenau = "0.0000";
        this.fToleranz = "1.0000";
        this.fTolRel = "False";
        this.nwGrenze = "0.0000";
        this.subOrt = "";
        this.kommentar = "";
        this.hoehe = "83";
        this.yTyp = "";
        this.xEinheit = "";
        this.quelle = "";
        this.publiziert = "F";
        this.parMerkmal = "";
        this.hauptReihe = "T";
        this.maxTextFocusStart = "1990-05-29T11:33:00Z";
        this.maxTextFocusEnd = "1995-06";
    }
}
