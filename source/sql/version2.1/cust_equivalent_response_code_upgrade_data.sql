INSERT INTO [dbo].[cust_equivalent_response_codes] ([code_isc],[description_isc],[code_iso],[code_b24],[date],[commision_value],[active],[process]) VALUES ('2326','TRN FALLBACK NO PERMITIDA','83','U8',GETDATE(),0,1,0)
INSERT INTO [dbo].[cust_equivalent_response_codes] ([code_isc],[description_isc],[code_iso],[code_b24],[date],[commision_value],[active],[process]) VALUES ('7261','NUMERO DE CHEQUEO NO NUMERICO','05','05',GETDATE(),0,1,1)
UPDATE cust_equivalent_response_codes SET process = '0' WHERE code_iso = '14'