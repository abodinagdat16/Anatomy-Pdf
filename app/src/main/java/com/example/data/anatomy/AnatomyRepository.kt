package com.example.data.anatomy

object AnatomyRepository {

    private val structures = mutableMapOf<String, AnatomyStructure>()

    init {
        registerBuiltInAtlas()
    }

    fun findStructure(query: String): AnatomyStructure? {
        val clean = query.trim().lowercase()
        // Direct ID match
        structures[clean]?.let { return it }

        // Exact name or latin match
        structures.values.firstOrNull {
            it.name.equals(clean, ignoreCase = true) ||
            it.latinName.equals(clean, ignoreCase = true) ||
            it.id.equals(clean, ignoreCase = true)
        }?.let { return it }

        // Substring / fuzzy match
        return structures.values.firstOrNull {
            clean.contains(it.name.lowercase()) ||
            it.name.lowercase().contains(clean) ||
            (clean.contains("carotid") && it.name.contains("Carotid", ignoreCase = true)) ||
            (clean.contains("sheath") && it.name.contains("Sheath", ignoreCase = true)) ||
            (clean.contains("vagus") && it.name.contains("Vagus", ignoreCase = true)) ||
            (clean.contains("jugular") && it.name.contains("Jugular", ignoreCase = true))
        }
    }

    fun getStructureById(id: String): AnatomyStructure? {
        return structures[id.lowercase()]
    }

    fun getAllStructures(): List<AnatomyStructure> {
        return structures.values.toList()
    }

    fun saveDynamicStructure(structure: AnatomyStructure) {
        structures[structure.id.lowercase()] = structure
    }

    private fun registerBuiltInAtlas() {
        // 1. Common Carotid Artery
        val cca = AnatomyStructure(
            id = "common_carotid_artery",
            name = "Common Carotid Artery",
            latinName = "Arteria carotis communis",
            category = StructureCategory.ARTERY,
            origin = "Right CCA arises from the Brachiocephalic Trunk (behind the right sternoclavicular joint). Left CCA arises directly from the Arch of the Aorta in the superior mediastinum.",
            termination = "Bifurcates at the superior border of the thyroid cartilage (vertebral level C3–C4 disc) into the Internal and External Carotid Arteries.",
            definition = "The major systemic arterial trunk supplying oxygenated blood to the head, neck, and brain. It ascends vertically in the anterior neck within the protective fascial investment of the carotid sheath.",
            course = "Ascends obliquely upward and laterally from behind the sternoclavicular joint toward the thyroid cartilage. Throughout its cervical course, it is enclosed in the Carotid Sheath alongside the Internal Jugular Vein (lateral) and Vagus Nerve (posterior). At the root of the neck, it is deeply placed behind the infrahyoid muscles and sternocleidomastoid (SCM); higher up at C4, it enters the Carotid Triangle where it becomes superficial and its pulse can be readily palpated.",
            relations = AnatomyRelations(
                anterior = listOf(
                    "Skin, superficial fascia, and Platysma muscle",
                    "Deep cervical fascia and anterior border of Sternocleidomastoid (SCM)",
                    "Sternohyoid, Sternothyroid, and superior belly of Omohyoid muscles",
                    "Ansa Cervicalis embedded on the anterior wall of the carotid sheath",
                    "Superior thyroid and middle thyroid veins crossing anteriorly"
                ),
                posterior = listOf(
                    "Transverse processes of C4–C6 cervical vertebrae (carotid tubercle of Chassaignac at C6)",
                    "Longus colli and Longus capitis muscles",
                    "Cervical Sympathetic Trunk lying posterior to the carotid sheath",
                    "Inferior Thyroid Artery arching medially behind lower part"
                ),
                medial = listOf(
                    "Larynx, Trachea, and Thyroid Gland lobe",
                    "Esophagus and Pharynx",
                    "Recurrent Laryngeal Nerve (in tracheoesophageal groove)"
                ),
                lateral = listOf(
                    "Internal Jugular Vein (IJV) - overlaps the artery anteriorly at lower neck",
                    "Vagus Nerve (Cranial Nerve X) - situated posterolaterally between artery & vein"
                )
            ),
            branches = listOf(
                BranchLink(
                    name = "Internal Carotid Artery (ICA)",
                    targetId = "internal_carotid_artery",
                    description = "Ascends without branches in the neck; enters skull through carotid canal to supply the cerebrum & eye (Circle of Willis)."
                ),
                BranchLink(
                    name = "External Carotid Artery (ECA)",
                    targetId = "external_carotid_artery",
                    description = "Gives off 8 branches in the neck and face, supplying the thyroid, tongue, face, scalp, and maxillary region."
                ),
                BranchLink(
                    name = "Carotid Sinus",
                    targetId = "carotid_sinus",
                    description = "Dilation at the origin of ICA/CCA bifurcation functioning as a high-pressure baroreceptor (innervated by CN IX)."
                ),
                BranchLink(
                    name = "Carotid Body",
                    targetId = "carotid_body",
                    description = "Small chemoreceptor nestled at the carotid bifurcation monitoring PaO2, PaCO2, and arterial pH."
                )
            ),
            clinicalCorrelations = listOf(
                "Carotid Pulse Palpation: Readily felt in the carotid triangle anterior to the SCM muscle at the level of the cricoid cartilage (C6 - Chassaignac's tubercle).",
                "Carotid Sinus Hypersensitivity & Syncope: Excessive pressure on the carotid sinus triggers intense vagal discharge leading to severe bradycardia and hypotension.",
                "Carotid Endarterectomy (CEA): Surgical removal of atheromatous plaques to prevent Transient Ischemic Attacks (TIA) and thromboembolic ischemic strokes.",
                "Carotid Bruit: Auscultated with stethoscope over the bifurcation indicating turbulent flow from arterial stenosis."
            ),
            mnemonics = "CCA Sheath Relations: 'Vagus sits Between, Artery is Medial, Vein is Lateral' (M-A-L-V).",
            highYieldSummary = "Bifurcates at C3/C4 (upper border of thyroid cartilage). Left arises directly from Aortic Arch (longer), Right from Brachiocephalic. Zero branches before terminal bifurcation.",
            images = listOf(
                AnatomyImage(
                    title = "Carotid Arteries & Branching Overview",
                    description = "Schematic of Common Carotid bifurcating into Internal and External Carotid Arteries at C4 level within the Carotid Triangle.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Gray513.png/800px-Gray513.png"
                ),
                AnatomyImage(
                    title = "Carotid Sheath Cross-Sectional Anatomy",
                    description = "Fascial arrangement: Medial Common Carotid Artery, Lateral Internal Jugular Vein, and Posterior Vagus Nerve.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Gray1195.png/800px-Gray1195.png"
                ),
                AnatomyImage(
                    title = "Carotid Triangle Boundaries & Neurovascular Contents",
                    description = "Boundaries formed by SCM, Omohyoid (superior belly), and Digastric (posterior belly) with carotid pulse point.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Gray1210.png/800px-Gray1210.png"
                )
            )
        )
        structures[cca.id] = cca
        structures["common carotid"] = cca
        structures["carotid artery"] = cca

        // 2. Carotid Sheath
        val carotidSheath = AnatomyStructure(
            id = "carotid_sheath",
            name = "Carotid Sheath",
            latinName = "Vagina carotica fasciae cervicalis",
            category = StructureCategory.FASCIA_SHEATH,
            origin = "Extends superiorly from the base of the skull (jugular foramen and carotid canal) down into the superior mediastinum to fuse with the adventitia of the great vessels and pericardium.",
            termination = "Blends inferiorly with the adventitia of the aortic arch and superior vena cava.",
            definition = "A dense tubular fascial investment of deep cervical fascia that envelops the major neurovascular bundle of the neck.",
            course = "Courses vertically along the anterolateral aspects of the cervical spine. Formed by contributions from all three layers of deep cervical fascia (investing layer anteriorly, pretracheal layer anteromedially, and prevertebral layer posteriorly).",
            relations = AnatomyRelations(
                anterior = listOf("Ansa Cervicalis nerve loop embedded on or within anterior sheath wall", "Sternocleidomastoid muscle", "Omohyoid muscle"),
                posterior = listOf("Cervical Sympathetic Trunk (embedded between posterior wall and prevertebral fascia)", "Transverse processes of cervical vertebrae"),
                medial = listOf("Trachea, Esophagus, Thyroid gland, Pharynx, Recurrent Laryngeal Nerve"),
                lateral = listOf("Deep cervical lymph nodes chain (along IJV)")
            ),
            branches = listOf(
                BranchLink("Common Carotid Artery (Medial)", "common_carotid_artery", "Lies medially in lower sheath; replaced by Internal Carotid superiorly."),
                BranchLink("Internal Jugular Vein (Lateral)", "internal_jugular_vein", "Lies laterally; significantly larger and distensible."),
                BranchLink("Vagus Nerve / CN X (Posterior)", "vagus_nerve", "Courses posterolaterally in the groove between artery and vein."),
                BranchLink("Ansa Cervicalis", "ansa_cervicalis", "Motor loop supplying infrahyoid strap muscles.")
            ),
            clinicalCorrelations = listOf(
                "Infection Conduit: The loose connective tissue plane surrounding the carotid sheath facilitates direct spread of deep neck abscesses (e.g. from peritonsillar/retropharyngeal space) down into the posterior mediastinum ('danger space').",
                "Central Venous Catheterization: Direct landmark guidance via ultrasound of the IJV within the carotid sheath to avoid inadvertent puncture of the high-pressure medial Common Carotid Artery."
            ),
            mnemonics = "Sheath Contents: 'I Just Vacated Medical Care' = IJV (Lateral), Vagus (Posterior), Medial Carotid (CCA/ICA).",
            highYieldSummary = "Contains CCA/ICA (medially), IJV (laterally), Vagus Nerve CN X (posterior groove), Deep cervical lymph nodes. Sympathetic chain is BEHIND the sheath, NOT inside.",
            images = listOf(
                AnatomyImage(
                    title = "Cross-Section of Deep Cervical Fascia & Sheath",
                    description = "Transverse cervical diagram showing fascial layers and sheath compartmentalization.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Gray1195.png/800px-Gray1195.png"
                )
            )
        )
        structures[carotidSheath.id] = carotidSheath
        structures["carotid sheath"] = carotidSheath

        // 3. Carotid Triangle
        val carotidTriangle = AnatomyStructure(
            id = "carotid_triangle",
            name = "Carotid Triangle",
            latinName = "Trigonum caroticum",
            category = StructureCategory.ANATOMICAL_SPACE,
            origin = "Anterior subdivision of the cervical triangle in the neck.",
            termination = "Extends from posterior belly of digastric to superior belly of omohyoid.",
            definition = "A crucial surgical landmark in the anterior neck where the bifurcation of the Common Carotid Artery and branches of External Carotid are accessible.",
            course = "Bounded superiorly by the posterior belly of the Digastric muscle and Stylohyoid; anteroinferiorly by the superior belly of the Omohyoid muscle; posteriorly by the anterior border of the Sternocleidomastoid (SCM) muscle.",
            relations = AnatomyRelations(
                contents = listOf(
                    "Common Carotid Artery & its bifurcation into ICA and ECA",
                    "Branches of External Carotid: Superior Thyroid, Ascending Pharyngeal, Lingual, Facial, Occipital",
                    "Internal Jugular Vein & its tributaries (Common Facial, Lingual, Superior Thyroid veins)",
                    "Vagus Nerve (CN X), Hypoglossal Nerve (CN XII), Accessory Nerve (CN XI)",
                    "Ansa Cervicalis (Superior and Inferior roots)",
                    "Deep Cervical Lymph Nodes (Jugulodigastric node)"
                )
            ),
            branches = listOf(
                BranchLink("Common Carotid Artery", "common_carotid_artery", "Bifurcates within this triangle at C4 level."),
                BranchLink("Internal Jugular Vein", "internal_jugular_vein", "Runs along the posterior boundary."),
                BranchLink("Hypoglossal Nerve (CN XII)", "hypoglossal_nerve", "Hooks around the occipital artery and crosses ECA/ICA."),
                BranchLink("Vagus Nerve (CN X)", "vagus_nerve", "Descends inside carotid sheath.")
            ),
            clinicalCorrelations = listOf(
                "Surgical Approach: Site for Carotid Endarterectomy, Carotid artery ligation, and vagal nerve stimulation implantations.",
                "Jugulodigastric Node: Located below posterior digastric belly; receives lymphatic drainage from palatine tonsils and tongue; enlarged in tonsillitis and oral carcinoma."
            ),
            mnemonics = "Triangle Boundaries: 'SO-D' = Superior belly Omohyoid, SCM anterior border, Digastric posterior belly.",
            highYieldSummary = "Surgical golden zone of neck: contains CCA bifurcation, 5 branches of ECA, CN X, XI, XII, and Ansa Cervicalis.",
            images = listOf(
                AnatomyImage(
                    title = "Triangles of the Anterior Neck",
                    description = "Carotid triangle shown bounded by Digastric, Omohyoid, and Sternocleidomastoid.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Gray1210.png/800px-Gray1210.png"
                )
            )
        )
        structures[carotidTriangle.id] = carotidTriangle
        structures["carotid triangle"] = carotidTriangle

        // 4. External Carotid Artery
        val eca = AnatomyStructure(
            id = "external_carotid_artery",
            name = "External Carotid Artery",
            latinName = "Arteria carotis externa",
            category = StructureCategory.ARTERY,
            origin = "Terminal branch of Common Carotid Artery at the level of upper thyroid cartilage (C3–C4).",
            termination = "Terminates within the parotid gland behind the neck of the mandible into Maxillary and Superficial Temporal arteries.",
            definition = "The major arterial vessel supplying external head structures, facial structures, scalp, tongue, and maxilla.",
            course = "Ascends anteromedially to the Internal Carotid Artery initially, then curves backwards and laterally to enter the substance of the Parotid Gland.",
            branches = listOf(
                BranchLink("Superior Thyroid Artery", "superior_thyroid_artery", "1st anterior branch; supplies thyroid apex & gives off Superior Laryngeal Artery."),
                BranchLink("Ascending Pharyngeal Artery", "ascending_pharyngeal_artery", "Small medial branch; supplies pharyngeal wall & meninges."),
                BranchLink("Lingual Artery", "lingual_artery", "2nd anterior branch; passes deep to hyoglossus to supply the tongue."),
                BranchLink("Facial Artery", "facial_artery", "3rd anterior branch; hooks around inferior mandibular border onto the face."),
                BranchLink("Occipital Artery", "occipital_artery", "1st posterior branch; runs in occipital groove to scalp."),
                BranchLink("Posterior Auricular Artery", "posterior_auricular_artery", "2nd posterior branch; supplies auricle and scalp behind ear."),
                BranchLink("Maxillary Artery", "maxillary_artery", "Larger terminal branch; enters infratemporal fossa (Middle Meningeal Artery)."),
                BranchLink("Superficial Temporal Artery", "superficial_temporal_artery", "Smaller terminal branch; palpated anterior to tragus.")
            ),
            clinicalCorrelations = listOf(
                "Ligation in Maxillofacial Trauma: Ligation in carotid triangle controls intractable maxillofacial hemorrhage.",
                "Giant Cell (Temporal) Arteritis: Involves Superficial Temporal Artery causing scalp tenderness, headache, and risk of blindness if ophthalmic branches occlude."
            ),
            mnemonics = "8 Branches Mnemonic: 'Some Anatomists Like Fucking, Others Prefer Many Students' (Superior thyroid, Ascending pharyngeal, Lingual, Facial, Occipital, Posterior auricular, Maxillary, Superficial temporal).",
            highYieldSummary = "Has 8 branches in neck & parotid (unlike ICA which has 0 branches in neck). Terminates into Maxillary & Superficial Temporal.",
            images = listOf(
                AnatomyImage(
                    title = "External Carotid Artery & Branches",
                    description = "Comprehensive view of the 8 branches of ECA distributed across neck, face, and scalp.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Gray513.png/800px-Gray513.png"
                )
            )
        )
        structures[eca.id] = eca
        structures["external carotid"] = eca
        structures["external carotid artery"] = eca

        // 5. Internal Carotid Artery
        val ica = AnatomyStructure(
            id = "internal_carotid_artery",
            name = "Internal Carotid Artery",
            latinName = "Arteria carotis interna",
            category = StructureCategory.ARTERY,
            origin = "Terminal branch of the Common Carotid Artery at C4 level.",
            termination = "Terminates at anterior perforated substance by dividing into Anterior Cerebral Artery (ACA) and Middle Cerebral Artery (MCA).",
            definition = "Primary arterial source supplying the cerebral hemispheres, orbits, and ophthalmic structures. Characteristically gives ZERO branches in the neck.",
            course = "Ascends vertically in carotid sheath through neck without branching. Enters petrous temporal bone via Carotid Canal, traverses cavernous sinus (s-shaped Carotid Siphon), pierces dura mater to enter subarachnoid space.",
            branches = listOf(
                BranchLink("Ophthalmic Artery", "ophthalmic_artery", "Enters orbit via optic canal; gives Central Retinal Artery."),
                BranchLink("Anterior Cerebral Artery (ACA)", "anterior_cerebral_artery", "Supplies medial aspect of cerebral hemisphere (leg/foot motor/sensory)."),
                BranchLink("Middle Cerebral Artery (MCA)", "middle_cerebral_artery", "Supplies lateral convexities of brain (arm/face/speech areas)."),
                BranchLink("Posterior Communicating Artery (PCom)", "pcom_artery", "Connects ICA to Posterior Cerebral Artery in Circle of Willis."),
                BranchLink("Anterior Choroidal Artery", "anterior_choroidal", "Supplies optic tract, hippocampus, and internal capsule posterior limb.")
            ),
            clinicalCorrelations = listOf(
                "Carotid-Cavernous Fistula: Traumatic basal skull fracture can rupture ICA within the cavernous sinus causing pulsating exophthalmos and orbital bruit.",
                "Berry Aneurysms: Saccular aneurysms at junction of ICA with Posterior Communicating Artery can cause CN III palsy with blown pupil and ptosis."
            ),
            mnemonics = "ICA Intracranial Branches: 'O-P-A-M-A' = Ophthalmic, Posterior communicating, Anterior choroidal, Middle cerebral, Anterior cerebral.",
            highYieldSummary = "ZERO branches in the neck. Enters skull via Carotid Canal. Bathes directly in venous blood of Cavernous Sinus alongside CN VI.",
            images = listOf(
                AnatomyImage(
                    title = "Circle of Willis & Internal Carotid Inflow",
                    description = "Cerebral arterial circle showing ICA giving off ACA, MCA, and Posterior Communicating branches.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2e/Circle_of_Willis_en.svg/800px-Circle_of_Willis_en.svg.png"
                )
            )
        )
        structures[ica.id] = ica
        structures["internal carotid"] = ica
        structures["internal carotid artery"] = ica

        // 6. Vagus Nerve (CN X)
        val vagus = AnatomyStructure(
            id = "vagus_nerve",
            name = "Vagus Nerve (Cranial Nerve X)",
            latinName = "Nervus vagus",
            category = StructureCategory.NERVE,
            origin = "Originates from medulla oblongata by a series of rootlets in the post-olivary sulcus.",
            termination = "Provides parasympathetic and visceral sensory innervation to thoracic and abdominal organs up to splenic flexure.",
            definition = "Tenth cranial nerve and primary parasympathetic carrier to heart, lungs, and digestive tract.",
            course = "Exits skull through middle compartment of Jugular Foramen. Enclosed in posterior groove of Carotid Sheath between CCA/ICA and IJV. Right vagus crosses anterior to subclavian artery (giving Right Recurrent Laryngeal Nerve hooking under it); Left vagus descends between Left CCA and Subclavian, crosses aortic arch (giving Left Recurrent Laryngeal Nerve hooking under ligamentum arteriosum).",
            relations = AnatomyRelations(
                anterior = listOf("Common Carotid Artery (medially)", "Internal Jugular Vein (laterally)"),
                posterior = listOf("Prevertebral fascia", "Sympathetic trunk"),
                contents = listOf("Superior Laryngeal Nerve (Internal & External)", "Recurrent Laryngeal Nerves", "Cardiac branches")
            ),
            branches = listOf(
                BranchLink("Superior Laryngeal Nerve", "superior_laryngeal_nerve", "Divides into Internal (sensory above vocal cords) and External (motor to Cricothyroid)."),
                BranchLink("Recurrent Laryngeal Nerve", "recurrent_laryngeal_nerve", "Motor to all intrinsic laryngeal muscles except Cricothyroid; sensory below vocal cords."),
                BranchLink("Cardiac Branches", "cardiac_plexus", "Supplies parasympathetic cardioinhibitory fibers to SA/AV nodes.")
            ),
            clinicalCorrelations = listOf(
                "Vagal Stimulation: Used in drug-resistant epilepsy and severe depression.",
                "Recurrent Laryngeal Nerve Injury: Thyroidectomy complication; unilateral injury causes hoarseness; bilateral causes acute airway obstruction / stridor.",
                "Vasovagal Syncope: Triggers bradycardia and peripheral vasodilation resulting in cerebral hypoperfusion."
            ),
            mnemonics = "Laryngeal Nerve Rule: 'All intrinsic laryngeal muscles supplied by Recurrent Laryngeal, EXCEPT Cricothyroid (External branch of Superior Laryngeal)'.",
            highYieldSummary = "Passes through Jugular Foramen. Sits in posterior groove of Carotid Sheath. Left RLN loops under Aortic Arch; Right RLN loops under Subclavian Artery.",
            images = listOf(
                AnatomyImage(
                    title = "Course of the Vagus Nerve in Neck & Mediastinum",
                    description = "Vagus nerve traversing carotid sheath and sending recurrent branches under great arteries.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a8/Gray791.png/800px-Gray791.png"
                )
            )
        )
        structures[vagus.id] = vagus
        structures["vagus nerve"] = vagus
        structures["cranial nerve x"] = vagus
        structures["cn x"] = vagus

        // 7. Internal Jugular Vein
        val ijv = AnatomyStructure(
            id = "internal_jugular_vein",
            name = "Internal Jugular Vein",
            latinName = "Vena jugularis interna",
            category = StructureCategory.VEIN,
            origin = "Begins at base of skull in posterior compartment of Jugular Foramen as direct continuation of Sigmoid Sinus (Superior bulb).",
            termination = "Joins the Subclavian Vein behind the sternal end of the clavicle to form the Brachiocephalic Vein (Inferior bulb).",
            definition = "The largest deep vein in the neck draining blood from the brain, face, and anterior neck.",
            course = "Descends vertically in the lateral part of the Carotid Sheath alongside Common Carotid / Internal Carotid Arteries and Vagus Nerve.",
            branches = listOf(
                BranchLink("Facial Vein", "facial_vein", "Drains anterior face; connects via angular vein to ophthalmic veins (danger triangle of face)."),
                BranchLink("Lingual Vein", "lingual_vein", "Drains tongue and floor of mouth."),
                BranchLink("Superior & Middle Thyroid Veins", "thyroid_veins", "Drain thyroid gland directly into IJV (Inferior thyroid vein drains to Brachiocephalic).")
            ),
            clinicalCorrelations = listOf(
                "Internal Jugular Central Line (CVC): Right IJV is preferred because it offers a direct, straight trajectory into the Superior Vena Cava and Right Atrium.",
                "Jugular Venous Pressure (JVP): Non-invasive reflection of right atrial pressure (measured vertically from sternal angle)."
            ),
            mnemonics = "Thyroid Venous Drainage: 'Superior & Middle go to IJV, Inferior goes to Innominate (Brachiocephalic)'.",
            highYieldSummary = "Continuation of sigmoid sinus. Lies LATERAL in carotid sheath. Merges with subclavian vein at venous angle (Pirogoff's angle) to form brachiocephalic.",
            images = listOf(
                AnatomyImage(
                    title = "Deep Veins of the Neck & Head",
                    description = "Internal jugular vein course and venous confluence with subclavian vein.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Gray1195.png/800px-Gray1195.png"
                )
            )
        )
        structures[ijv.id] = ijv
        structures["internal jugular"] = ijv
        structures["internal jugular vein"] = ijv

        // 8. Circle of Willis
        val circleOfWillis = AnatomyStructure(
            id = "circle_of_willis",
            name = "Circle of Willis (Cerebral Arterial Circle)",
            latinName = "Circulus arteriosus cerebri",
            category = StructureCategory.ARTERY,
            origin = "Anastomotic polygon located in the interpeduncular cistern at the base of the brain.",
            termination = "Equalizes blood pressure between carotid and vertebrobasilar systems.",
            definition = "A vital arterial anastomosis encircling the optic chiasm and pituitary stalk that provides collateral circulation to the cerebral hemispheres.",
            course = "Formed anteriorly by Anterior Cerebral Arteries (ACA) connected by Anterior Communicating Artery (ACom); laterally by Internal Carotid Arteries (ICA); posteriorly by Posterior Cerebral Arteries (PCA) connected to ICA via Posterior Communicating Arteries (PCom).",
            branches = listOf(
                BranchLink("Anterior Communicating Artery (ACom)", "acom", "Most common site of Berry saccular aneurysms (bitemporal hemianopsia / ACA ischemia)."),
                BranchLink("Posterior Communicating Artery (PCom)", "pcom", "Second most common site of Berry aneurysms (causes CN III palsy with mydriasis)."),
                BranchLink("Basilar Artery", "basilar_artery", "Formed by junction of vertebral arteries at pontomedullary junction.")
            ),
            clinicalCorrelations = listOf(
                "Ruptured Saccular (Berry) Aneurysm: Causes severe 'worst headache of life' Subarachnoid Hemorrhage (SAH) and bloody lumbar puncture.",
                "Ischemic Stroke Collateralization: Can maintain perfusion when one carotid becomes slowly occluded over time."
            ),
            mnemonics = "Circle Components: '2 ACAs, 1 ACom, 2 ICAs, 2 PComs, 2 PCAs, 1 Basilar feeder'.",
            highYieldSummary = "Key basal anastomosis. ACA + ACom + ICA + PCom + PCA. Berry aneurysm in ACom = visual defects; Berry aneurysm in PCom = CN III oculomotor palsy.",
            images = listOf(
                AnatomyImage(
                    title = "Circle of Willis Diagram",
                    description = "Anatomical schematic showing anterior and posterior circulations and communicating vessels.",
                    imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2e/Circle_of_Willis_en.svg/800px-Circle_of_Willis_en.svg.png"
                )
            )
        )
        structures[circleOfWillis.id] = circleOfWillis
        structures["circle of willis"] = circleOfWillis
    }
}
