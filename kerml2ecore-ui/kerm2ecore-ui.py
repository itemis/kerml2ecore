import streamlit as st
import requests

input_file = st.file_uploader("Zip with KERML")

st.header("Upload KERML file to API")
if input_file is not None:
    st.info("File uploaded successfully")
    if st.button("Upload to API"):
        st.info("Uploading file to API...")
        file_bytes = input_file.getvalue()
        api_token = "blablabla"
        file_upload = requests.post(
            url="http://localhost:8080/kerml2ecore",
            files={'file': file_bytes}#,
            # headers={'Content-Type': 'multipart/form-data'}
        )

        st.download_button('Download', file_upload.text, "schnitzel.ecore")
