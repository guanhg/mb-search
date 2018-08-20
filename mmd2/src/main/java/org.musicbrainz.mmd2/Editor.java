//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.06.24 at 05:07:54 PM CDT 
//


package org.musicbrainz.mmd2;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}name"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}member-since" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}privs" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}gender" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}age" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}homepage" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}bio" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}area" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}language-list" minOccurs="0"/&gt;
 *         &lt;element ref="{http://musicbrainz.org/ns/mmd-2.0#}edit-information" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attGroup ref="{http://musicbrainz.org/ns/mmd-2.0#}def_editor-attribute_extension"/&gt;
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "name",
    "memberSince",
    "privs",
    "gender",
    "age",
    "homepage",
    "bio",
    "area",
    "languageList",
    "editInformation"
})
@XmlRootElement(name = "editor")
public class Editor {

    @XmlElement(required = true)
    protected String name;
    @XmlElement(name = "member-since")
    protected String memberSince;
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger privs;
    protected Gender gender;
    protected String age;
    @XmlSchemaType(name = "anyURI")
    protected String homepage;
    protected String bio;
    protected DefAreaElementInner area;
    @XmlElement(name = "language-list")
    protected LanguageList languageList;
    @XmlElement(name = "edit-information")
    protected EditInformation editInformation;
    @XmlAttribute(name = "id")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger id;
    @XmlAttribute(name = "score", namespace = "http://musicbrainz.org/ns/ext#-2.0")
    protected Integer score;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the memberSince property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMemberSince() {
        return memberSince;
    }

    /**
     * Sets the value of the memberSince property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMemberSince(String value) {
        this.memberSince = value;
    }

    /**
     * Gets the value of the privs property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getPrivs() {
        return privs;
    }

    /**
     * Sets the value of the privs property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setPrivs(BigInteger value) {
        this.privs = value;
    }

    /**
     * Gets the value of the gender property.
     * 
     * @return
     *     possible object is
     *     {@link Gender }
     *     
     */
    public Gender getGender() {
        return gender;
    }

    /**
     * Sets the value of the gender property.
     * 
     * @param value
     *     allowed object is
     *     {@link Gender }
     *     
     */
    public void setGender(Gender value) {
        this.gender = value;
    }

    /**
     * Gets the value of the age property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAge() {
        return age;
    }

    /**
     * Sets the value of the age property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAge(String value) {
        this.age = value;
    }

    /**
     * Gets the value of the homepage property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHomepage() {
        return homepage;
    }

    /**
     * Sets the value of the homepage property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHomepage(String value) {
        this.homepage = value;
    }

    /**
     * Gets the value of the bio property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBio() {
        return bio;
    }

    /**
     * Sets the value of the bio property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBio(String value) {
        this.bio = value;
    }

    /**
     * Gets the value of the area property.
     * 
     * @return
     *     possible object is
     *     {@link DefAreaElementInner }
     *     
     */
    public DefAreaElementInner getArea() {
        return area;
    }

    /**
     * Sets the value of the area property.
     * 
     * @param value
     *     allowed object is
     *     {@link DefAreaElementInner }
     *     
     */
    public void setArea(DefAreaElementInner value) {
        this.area = value;
    }

    /**
     * Gets the value of the languageList property.
     * 
     * @return
     *     possible object is
     *     {@link LanguageList }
     *     
     */
    public LanguageList getLanguageList() {
        return languageList;
    }

    /**
     * Sets the value of the languageList property.
     * 
     * @param value
     *     allowed object is
     *     {@link LanguageList }
     *     
     */
    public void setLanguageList(LanguageList value) {
        this.languageList = value;
    }

    /**
     * Gets the value of the editInformation property.
     * 
     * @return
     *     possible object is
     *     {@link EditInformation }
     *     
     */
    public EditInformation getEditInformation() {
        return editInformation;
    }

    /**
     * Sets the value of the editInformation property.
     * 
     * @param value
     *     allowed object is
     *     {@link EditInformation }
     *     
     */
    public void setEditInformation(EditInformation value) {
        this.editInformation = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setId(BigInteger value) {
        this.id = value;
    }

    /**
     * Gets the value of the score property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getScore() {
        return score;
    }

    /**
     * Sets the value of the score property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setScore(Integer value) {
        this.score = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * 
     * <p>
     * the map is keyed by the name of the attribute and 
     * the value is the string value of the attribute.
     * 
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     * 
     * 
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
