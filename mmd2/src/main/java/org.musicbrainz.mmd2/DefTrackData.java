//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.06.24 at 05:07:54 PM CDT 
//


package org.musicbrainz.mmd2;

import javax.xml.bind.annotation.*;
import java.math.BigInteger;


/**
 * <p>Java class for def_track-data complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="def_track-data"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}position" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}number" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}title" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}length" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}artist-credit" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}recording" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "def_track-data", propOrder = {
    "position",
    "number",
    "title",
    "length",
    "artistCredit",
    "recording"
})
public class DefTrackData {

    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger position;
    protected String number;
    protected String title;
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger length;
    @XmlElement(name = "artist-credit")
    protected ArtistCredit artistCredit;
    protected Recording recording;
    @XmlAttribute(name = "id")
    @XmlSchemaType(name = "anyURI")
    protected String id;

    /**
     * Gets the value of the position property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getPosition() {
        return position;
    }

    /**
     * Sets the value of the position property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setPosition(BigInteger value) {
        this.position = value;
    }

    /**
     * Gets the value of the number property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNumber() {
        return number;
    }

    /**
     * Sets the value of the number property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNumber(String value) {
        this.number = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the length property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getLength() {
        return length;
    }

    /**
     * Sets the value of the length property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setLength(BigInteger value) {
        this.length = value;
    }

    /**
     * Gets the value of the artistCredit property.
     * 
     * @return
     *     possible object is
     *     {@link ArtistCredit }
     *     
     */
    public ArtistCredit getArtistCredit() {
        return artistCredit;
    }

    /**
     * Sets the value of the artistCredit property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArtistCredit }
     *     
     */
    public void setArtistCredit(ArtistCredit value) {
        this.artistCredit = value;
    }

    /**
     * Gets the value of the recording property.
     * 
     * @return
     *     possible object is
     *     {@link Recording }
     *     
     */
    public Recording getRecording() {
        return recording;
    }

    /**
     * Sets the value of the recording property.
     * 
     * @param value
     *     allowed object is
     *     {@link Recording }
     *     
     */
    public void setRecording(Recording value) {
        this.recording = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
