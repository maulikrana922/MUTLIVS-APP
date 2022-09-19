package com.es.multivs.presentation.view.fragments

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.es.multivs.databinding.TermsAndConditionsLayoutBinding
import com.es.multivs.data.utils.autoCleared

/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
class TermsAndConditionFragment : DialogFragment() {

    private var binding: TermsAndConditionsLayoutBinding by autoCleared()

    companion object {
        const val TAG = "TermsAndConditionFragment"
        fun newInstance(): TermsAndConditionFragment = TermsAndConditionFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TermsAndConditionsLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.termsAndConditionsTv.text = Html.fromHtml(data, Html.FROM_HTML_MODE_COMPACT);
        } else {
            binding.termsAndConditionsTv.text = Html.fromHtml(data);
        }

        binding.closeBtn.setOnClickListener {
            dismiss()
        }

    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
    }

    var data = """<p align="center" dir="LTR">
    <strong><u>Terms and Conditions</u></strong>
</p>
<p align="center" dir="LTR">
    <strong><u></u></strong>
</p>
<p dir="LTR">
    <strong>Use of the website located at </strong>
        <strong>https://us.etrogsystems.com/etrogsystems</strong>

    <strong>
        (or such other urls with respect to  Etrog, as those terms
        are defined below, the "Website") and/or any of the Etrog
        Applications (each an “App”) and the platform contained on the Website
        and/or App and devices supplied by 
    </strong>
 
    <strong>
        Etrog or on its behalf (together, the “Platform”) are subject to the
        Terms and Conditions set forth below which may be changed and Etrog Systems Ltd.
    </strong>
(“<strong>Etrog</strong>” or “<strong>Company</strong>” or “<strong>Us</strong>” or “<strong>We</strong>”    <strong>as applicable</strong>)
    <strong>
        from time to time. These Terms and Conditions constitute a binding
        agreement (the “Agreement” or “Terms”) between you
    </strong>
(“<strong>You</strong>” or “<strong>User</strong>”)    <strong>and Us.</strong>
</p>
<p dir="LTR">
    By choosing to use the Platform, you agree to abide by all terms and
    conditions of this Agreement. Etrog may change, add or remove
    portions of this Agreement at any time, in its sole discretion without
    notice nor liability.
</p>
<p dir="LTR">
    If any of these rules or their future changes are unacceptable to you, you
    should immediately cease to use or view the Website and/or App and its
    contents, including the Platform. Your use or continued use of the Platform
    (“<strong>Services</strong>”), now or following the posting of notice of
    any changes in these Terms and Conditions, will indicate acceptance by you
    of such then existing rules, changes, or modifications.
</p>
<p dir="LTR">
    1. <u>General</u>
</p>
<p dir="LTR">
    <u></u>
</p>
<p dir="LTR">
    1.1 As a condition for use of the Platform, User must register in the
    Platform and agree to the Terms. By clicking on the Log In tab you agree to
    be bound by the Terms.
</p>
<p dir="LTR">
    1.2 The Company reserves the right to update and change these Terms at any
    time. Any changes to the Terms shall be notified to User. Should User not
    agree to the revised Terms, User is prohibited from using and/or accessing
    the Platform via the Website and/or App. It is hereby clarified that
    continued use of the Platform shall constitute User’s consent to the
    revised Terms.
</p>
<p dir="LTR">
    1.3 This Agreement will apply indefinitely any prospective termination by
    either party will be subject to the Terms. The User may discontinue use of
    the Platform at any time and such discontinuation shall constitute
    termination of the Services, effective as of the date that the Company
    receives User notice of termination. Notwithstanding the aforementioned,
    discontinuation of the Platform or other termination of these Terms and the
    Services, does not impact User’s obligations as they are outstanding as of
    such termination nor our rights and provisions which expressly survive
    termination shall continue to be binding on the User.
</p>
<p dir="LTR">
    1.4 These Terms apply to all parts of the Platform, content, and/or
    products and services provided thereby.
</p>
<p dir="LTR">
    1.5 Words and defined terms denoting the singular include the plural and
    vice versa and the use of any gender shall be applicable to all genders.
</p>
<p dir="LTR">
    1.6 If any provision of these Terms shall be judicially determined to be
    invalid, illegal or unenforceable, the validity, legality and
    enforceability of the remaining provisions shall not in any way be affected
    or impaired thereby.
</p>
<p dir="LTR">
    2. <u>Access to Platform</u>
</p>
<p dir="LTR">
    2.1. Access to the Platform is contingent upon User providing personal,
    including medically confidential, information and may involve the
    assignment of a username and/or password, all at the Company’s sole and
    absolute discretion. User agrees that the User is only entitled to one
    username and such username is personal, non-transferrable and
    non-assignable. Moreover, User undertakes not to share/reveal its username
    nor password to any other party.
</p>
<p dir="LTR">
    2.2. When submitting personal information upon registration or otherwise,
    User agrees that the Company may retain, use and/or transfer the
    information at the Company's sole discretion.
</p>
<p dir="LTR">
    2.3 Following registration for a Platform account, User is solely liable
    for maintaining the confidentiality of such account and password and for
    restricting access to the Platform. User agrees to accept liability for all
    activities that occur under the User account or password. Company cannot
    and will not be liable for any loss or damage arising from User’s failure
    to comply with these obligations. Company reserves the right to delete or
    change any username or password at any time and for any reason.
</p>
<p dir="LTR">
    3. <u>Platform Services</u>
</p>
<p dir="LTR">
    3.1. The Platform is intended to be used for measuring, displaying,
    reviewing and storing of medical information of adults. The Platform is not
    intended to substitute for a hospital diagnostic ECG device and is only to
    be used strictly in compliance with doctors’ orders. The purpose of the
    Platform is to provide remote measuring, monitoring, and streaming of adult
patients’ medical data to health professionals (the “    <strong>Purpose</strong>”). Any use of the Platform by User that deviates
    from this Purpose or any provisions of these Terms, will be deemed a misuse
    of the Platform and subject the User’s account to immediate termination.
</p>
<p dir="LTR">
    3.2. By registering for the Platform, the User declares that s/he is over
    18 years old and has the required legal capacity to use the Platform.
</p>
<p dir="LTR">
    3.3. The Platform and/or its contents shall not be used in any way to hurt
    and/or harm any person, their privacy and/or any other rights, and use of
    the Platform and/or the content that contravenes these Terms and/or the
    provisions of any law, practice or public regulation, will not be tolerated
    nor permitted.
</p>
<p dir="LTR">
    3.4. Company reserves the right to report to the relevant authorities any
    breach of these Terms, improper use, or any other illegal and/or abusive
    behavior.
</p>
<p dir="LTR">
    3.5. Any use of the Platform and/or the content contained therein is for
    personal use only, and other use, including business, marketing or
    commercial use shall not be allowed, unless otherwise permitted by the
    Company with express, advanced, written consent. Any such consent granted
    by the Company shall be non-transferable.
</p>
<p dir="LTR">
    3.6. Any such consent shall not be used to derogate from the Company's
    rights, including ownership rights in particular and intellectual property
    rights in general, and will not imply any act or omission of the Company
    for any consent to transfer and/or use of any intellectual property or
    other right of the licensee or to another person.
</p>
<p dir="LTR">
    3.7. Use of the Platform by User must be within “reasonable” limits. For
    this purpose, reasonable use shall be as defined by any applicable law. The
    User may not make nor alter any content or make any other modification on
    the Platform, including but not limited to any accompanying devices,
    accessories, hardware and/or software.
</p>
<p dir="LTR">
    3.8. Company reserves the right to block any User from using the Platform
    that does not comply with these Terms and/or uses the Platform for illegal
    purposes, including hacking, harassment and/or impersonation, and any
    conduct that the Company deems offensive.
</p>
<p dir="LTR">
    3.9. The Company may change, add, subtract, delete, alter and/or update
    from time to time the contents and/or the appearance of the Platform and/or
    how it is used and/or the products and/or devices contained therein,
    without prior notice and at the Company’s sole discretion.
</p>
<p dir="LTR">
    4. <u>Platform Advertisements</u>
</p>
<p dir="LTR">
    <u></u>
</p>
<p dir="LTR">
    4.1. Company reserves the right to include in the Platform any content that
    constitutes an advertisement by the Company and/or by any third party
    and/or any other commercial content (the “<strong>Advertisements</strong>
    ”).
</p>
<p dir="LTR">
    4.2. The advertisers shall be solely liable for any information and/or
    content displayed and/or provided by the Advertisements that appear in the
    Platform and/or shall be sent to the Platform’s Users and the Company shall
    not bear any liability in this regard.
</p>
<p dir="LTR">
    4.3. Images or illustrations appearing in the Platform are for illustration
    purposes only and the Company shall not bear any liability in this regard.
</p>
<p dir="LTR">
    5. <u>Content Availability</u>
</p>
<p dir="LTR">
    If there are any malfunctions of any kind whatsoever in the Platform, which
    may prevent or impede access to and/or use of the Platform, including due
    to communication difficulties and/or routine maintenance and/or other
    reasons, and the use of the Platform may be terminated and/or discontinued
    without completion or retention and the Company will be exempt from any
    liability in this regard. The User is solely liable for preserving any
    information that it may require from the Platform.
</p>
<br clear="all"/>
<p dir="LTR">
    6. <u>Device Function</u>
</p>
<p dir="LTR">
    <u></u>
</p>
<p dir="LTR">
    Without derogating of the foregoing, User acknowledges that in some cases,
    the use of the Platform depends on the proper functioning of a mobile
    device and the components contained therein, including but not limited to
    device audio and video communication technology, as well as an internet
    connection. User understands and acknowledges that the quality, inaccuracy,
    disruption or malfunction of the foregoing may interfere with use of the
    Platform. Company shall be exempt from any claim regarding any malfunction
    resulting from use of the Platform through a defective device or a mobile
    device that does not support the Platform.
</p>
<p dir="LTR">
    Use of the Internet is solely at User’s own risk and is subject to all
    applicable state, national and international laws and regulations. Neither
    Company nor its affiliates will be liable for any loss resulting from a
    cause over which they do not have direct control, including but not limited
    to failure of electronic or mechanical equipment or communication lines,
    telephone or other interconnect problems, computer viruses, unauthorized
    access, theft, operator errors, severe weather, earthquakes, natural
    disasters, strikes or other labor problems, wars, or governmental
    restrictions.
</p>
<p dir="LTR">
    Company makes no representation that content provided on the Platform is
    applicable or appropriate for use in any location outside of the United
    States. Company assumes no responsibility for User’s use of any of the
    content provided on the Platform.
</p>
<p dir="LTR">
    7.
    <a name="No_Medical_Services_or_Advice">
        <u>No Medical Services or Advice</u>
    </a>
    <u>/HIPPA</u>
    <strong></strong>
</p>
<p dir="LTR">
    Nothing contained, expressed or implied in the Platform is intended as, nor
    shall be construed as, medical advice. No doctor-patient relationship is
    established between Company and User by way of use of the Platform or under
    any circumstances whatsoever. Individual inquiries about medical issues, or
    sensitive or confidential matters should be addressed to appropriate health
    care professionals.
</p>
<p dir="LTR">
With respect to the Health Insurance Portability and Accountability Act (“<strong>HIPAA</strong>”), User hereby authorizes  
    Etrog  through use of the Platform by or on behalf of User and
    agrees that Etrog  may release any of such information, whether
    or not protected, to third parties provided that User may cancel this
    authorization prospectively upon prior written notice to Etrog.
    User understands that information used or stored by Etrog 
    regarding the User, may be protected information yet User authorizes Etrog
   to use the information and to share the information with others
    at its discretion, so long as it complies with HIPPA rules relating to
    Protected Health Information (PHI).
</p>
<p dir="LTR">
8. <a name="No_Legal_Advice"><u>No Legal Advice</u></a>    <strong><u></u></strong>
</p>
<p dir="LTR">
    Nothing contained, expressed, or implied in the Platform is intended as,
    nor shall be construed or understood as, legal advice, guidance, or
    interpretation. No attorney-client relationship is established between
    Company and User by way of use of the Platform or under any circumstances
    whatsoever. If User has questions about any law, statute, regulation, or
    requirement expressly or implicitly referenced in the Platform, User should
    contact his/her own legal counsel.
</p>
<p dir="LTR">
9.<a name="Confidentiality_Cannot_be_Guaranteed"><u>Confidentiality</u></a>    <strong></strong>
</p>
<p dir="LTR">
    Please be advised that the confidentiality of any communication or material
    transmitted to the Company via the Platform cannot be guaranteed, including
    not limited to personal information, medical information, and location
    tracking.
</p>
<p dir="LTR">
    You acknowledge and agree that all trade secrets, patents, patent
    applications, copyrights, know-how, processes, ideas, inventions (whether
    patentable or not), formulas, computer programs, databases, technical
    drawings, designs, algorithms, technology, circuits, layouts, interfaces,
    materials, schematics any other technical, business, financial, customer
    and product development information and other confidential information
    pertaining to the Products, are the sole property of the Company and you
    have no proprietary rights in any of the aforesaid.
</p>
<p dir="LTR">
    You agree not to reverse engineer, decompile, or disassemble the software
    nor the hardware of the Product except to the extent that such activity is
expressly permitted by applicable law notwithstanding this limitation.    <a name="Disclaimer_of_Warranty_and_Liability"></a>
</p>
<p dir="LTR">
    10. <u>Privacy Notice</u>
</p>
<p dir="LTR">
    <u></u>
</p>
<p dir="LTR">
    <u></u>
</p>
<p dir="LTR">
    User acknowledges and agrees that the Company and/or Platform shall collect
    and store and information that User enters on the Platform or sends to the
    Company in any manner. User may choose not to provide and/or to withhold
    certain information and understands and consents to such choice affecting
    and limiting various Platform features.
</p>
<p dir="LTR">
    Patient Users acknowledge and agree that by agreeing to these Terms, User
    waives confidentiality, expressly including but not limited to medical
    confidentiality, vis-à-vis health care provider Users also using the
    Platform.
</p>
<p dir="LTR">
    The Company releases User account and other information when Company
    believes release it is appropriate to comply with the law; enforce or apply
    our Terms and other agreements; or protect the rights, property, or safety
    of the Company and/or the Platform, the Users, and/or others.
</p>
<p dir="LTR">
    You grant to the Company access to your medical/health condition, you are
    aware of the provisions of HIPAA regulations and you hereby waive any and
    all claims against the Company, its shareholders, directors, managers and
    representatives in that regard.
</p>
<p dir="LTR">
    11. <u>Disclaimer of Warranty and Liability</u><strong><u></u></strong>
</p>
<p dir="LTR">
    <strong>
        USE OF THE PLATFOM IS ENTIRELY AT USER’S OWN RISK. NEITHER COMPANY NOR
        ITS AFFILIATES, SHAREHOLDERS, DIRECTORS, OFFICERS AND REPRESENTATIVES
        ARE RESPONSIBLE FOR THE CONSEQUENCES OF RELIANCE ON ANY INFORMATION
        CONTAINED IN OR SUBMITTED TO THE PLATFORM, AND THE RISK OF INJURY FROM
        THE FOREGOING RESTS ENTIRELY WITH THE USER. THE PLATFORM IS PROVIDED
        "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
        INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE, NOR NON-INFRINGEMENT. UNDER NO
        CIRCUMSTANCES SHALL THE COMPANY BE LIABLE FOR ANY DIRECT, SPECIAL,
        INDIRECT, INCIDENTAL, CONSEQUENTIAL, NOR PUNITIVE DAMAGES, INCLUDING
        WITHOUT LIMITATION, LOSS OF REVENUES OR LOST PROFITS, WHICH MAY RESULT
        FROM THE USE OF, ACCESS TO, OR INABILITY TO USE THESE MATERIALS AND/OR
        THE PLATFORM OR PLATFORM INFORMATION.
    </strong>
</p>
<p dir="LTR">
    <strong>
        UNDER NO CIRCUMSTANCES WILL THE TOTAL LIABILITY OF COMPANY TO YOU
        EXCEED THE PRICE PAID BY YOU TO THE COMPANY FOR USE OF AN INDIVIDUAL
        PLATFORM.
    </strong>
</p>
<p dir="LTR">
    <strong>
        <u>
            USE OF THE PLATFORM IS NOT APPROPRIATE FOR EMERGENCIES. IF YOU
            THINK YOU HAVE A MEDICAL OR MENTAL HEALTH EMERGENCY, OR IF AT ANY
            TIME YOU ARE CONCERNED ABOUT YOUR CARE OR TREATMENT, CALL 911 OR GO
            TO THE NEAREST OPEN CLINIC OR EMERGENCY ROOM.
        </u>
    </strong>
</p>
<p dir="LTR">
    User acknowledges and agrees that Company’s suppliers are third-party
    beneficiaries of these Terms, with the right to enforce the limitations of
    warranty and liability set forth herein with respect to the respective
    Platform technology of such suppliers and Company.
</p>
<p dir="LTR">
    12. <u>Relationship of Parties</u>
</p>
<p dir="LTR">
    <u></u>
</p>
<p dir="LTR">
    12.1. User acknowledges that nothing in these Terms herein shall be
    construed to create a partnership, joint venture, or agency relationship
    between us and you.
</p>
<p dir="LTR">
    12.2. User recognizes that the Platform serves to link between medical
    patients and health professionals. The Company shall have no vicarious
    liability for any damage caused by the information input and/or output
    entered by the User in the Platform and any action and/or inaction of User
    and/or medical/health professionals caused thereby.
</p>
<p dir="LTR">
    12.3. Company shall not be liable for any contractual and/or tort liability
    or any other liability for use and/or misuse of the Platform and/or its
    implementation and/or its contents, including without limitation, any
    theft, burglary, physical injury, property damage, and/or third party
    liability nor for an injury incurred by the User in connection, both direct
    and indirect with the Platform, including but not limited to any Platform
    accessory and/or device.
</p>
<p dir="LTR">
    12.4. Company shall not be liable for any content actively or passively
    provided or entered by the User, its legality, correctness, dependability,
    accuracy, completeness or any file attached thereto, and any damage, loss,
    pain and suffering and/or results caused, directly or indirectly, to the
    User and/or to third parties.
</p>
<p dir="LTR">
    12.5. Company will not be liable for any damages due to use of the Platform
    and/or damages caused by the content being published by the User.
</p>
<p dir="LTR">
    12.6. Saving and use of User’s content is subject to the provisions of the
    relevant laws and these Terms.
</p>
<p dir="LTR">
    12.7. The User hereby declares that any liability and risk for any damage
    and/or loss incurred as a result of using the Platform shall be borne by
    the User, including but not limited to physical accidents, mental anguish
    and/or any material damage.
</p>
<p dir="LTR">
    It should also be noted that using your mobile device while driving is
    prohibited and dangerous to User and to User’s environment, and the Company
    will not be held responsible for any damage that may be caused by using the
    Platform through the mobile device while driving.
</p>
<p dir="LTR">
    13. <u>Indemnity</u>
</p>
<p dir="LTR">
    <u></u>
</p>
<p dir="LTR">
    User undertakes to indemnify and compensate the Company and/or its
    stockholders, directors, officers and representatives for any act and/or
    default that caused the Company and/or third party direct and/or indirect
    damage, loss, loss of profit, payment or expense, whether or not due to
    these Terms and regardless of whether the User violated any law or due to
    any claim or action taken by any third party.
</p>
<p dir="LTR">
    14. <u>Governing Law</u>
</p>
<p dir="LTR">
    <u></u>
</p>
<p dir="LTR">
    The provisions of these Terms and all actions arising out of or in
    connection with these Terms shall be governed by and construed in
    accordance with the laws of the State of Israel. All disputes, differences
    or questions arising out of or relating to these Terms, or pertaining to
    their validity, interpretation, breach or violation shall be decided
    exclusively by the appropriate court sitting in New York, New York, USA.
</p>
<p dir="LTR">
    <strong>Miscellaneous</strong>
</p>
<p dir="LTR">
    This Agreement constitutes the entire agreement between the parties, and
    supersedes all prior written or oral agreements or communications with
    respect to the subject matter herein. If any term in this Agreement is
    declared unlawful, void or for any reason unenforceable by any court, then
    such term will be deemed severable from the remaining terms and will not
    affect the validity and enforceability of such remaining terms. The section
    headings in this Agreement are for convenience only and must not be given
    any legal import.
</p>
<p dir="LTR">
    TRADEMARKS
</p>
<p dir="LTR">
    <a name="_Hlk49405648"><strong>© 2020 </strong></a>
    Etrog Systems Ltd. and All product or
    service names mentioned herein or on the Website and/or App are or may be
trademarks or registered trademarks of their respective owners. Etrog <u> </u>is a registered
    trademark. The products or services described in this document may be
    protected by Israeli patents, U.S. patents, foreign patents, or pending
    applications.
</p>
<p align="center" dir="LTR">
    <strong></strong>
</p>
<p align="center" dir="LTR">
    <strong>
        <br/>
        ANY RIGHTS NOT EXPRESSLY GRANTED HEREIN ARE RESERVED BY
    </strong>
</p>
<p align="center" dir="LTR">
    <strong>Etrog systems Ltd.</strong>
</p>"""
}