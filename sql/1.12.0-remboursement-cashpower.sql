-- adminmobile 1.12.0 : statut REMBOURSE des achats CashPower deja annules.
-- Meme traitement que le bouton "Mettre a jour tous les statuts" de l'ecran
-- Annulation Operations > Annul Op CashPower : l'un ou l'autre, une seule fois suffit
-- (sans effet sur la comptabilite, peut etre relance sans risque).
-- LIBELLE_ECRITURE n'est pas indexee : un parcours complet de SPT.MOUVEMENT (environ une minute).
-- Octobre 2026 : environ 152 achats concernes (149 FAILED, 1 PENDING...).

MERGE INTO SPT.CASHPOWER cp
USING (SELECT DISTINCT TO_NUMBER(REGEXP_SUBSTR(libelle_ecriture, '[0-9]+$')) AS numero_operation
         FROM SPT.MOUVEMENT
        WHERE libelle_ecriture LIKE 'ANNULATION OP %') a
   ON (cp.numero_operation = a.numero_operation)
 WHEN MATCHED THEN UPDATE
      SET cp.statut = 'REMBOURSE'
    WHERE cp.statut IS NULL OR cp.statut <> 'REMBOURSE';

-- Verifier le nombre de lignes fusionnees avant de valider :
COMMIT;
