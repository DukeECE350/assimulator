package assemble;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import db.JsonTransformer;
import io.Stringer;
import models.Asm;
import models.IntLine;
import models.Mif;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * Created by jiaweizhang on 4/11/16.
 */
public class AssemblerService {
    private final DB db;
    private final DBCollection assemblerasms;
    private final DBCollection assemblermifs;

    public AssemblerService(DB db) {
        this.db = db;
        this.assemblerasms = db.getCollection("assemblerasms");
        this.assemblermifs = db.getCollection("assemblermifs");
        setupEndpoints();
    }

    private void setupEndpoints() {
        post("/api/assemble", (req, res) -> {
            String[] arr = req.body().split("\n");
            List<String> list = new ArrayList<String>(Arrays.asList(arr));

            ObjectId asmId = createAsm(req.body());
            res.status(201);

            String result = assemble(list);

            ObjectId mifId = createMif(result);
            AssemblerResponse ar = new AssemblerResponse();
            ar.setAsmId(asmId.toString());
            ar.setMifId(mifId.toString());
            ar.setMif(result);
            return ar;
        }, new JsonTransformer());

        get("/files/assembler/asm/:id/file.asm", (req, res) -> {
            //res.header("Content-Disposition", "attachment; filename=\"file.asm\"");
            return findAsm(req.params(":id")).getAsm();
        });

        get("/files/assembler/mif/:id/imem.mif", (req, res) -> {
            //res.header("Content-Disposition", "attachment; filename=imem.mif");
            return findMif(req.params(":id")).getMif();
        });
    }

    private String assemble(List<String> strings) {
        Assembler a = new ECE350Assembler();
        List<IntLine> ints = a.parse(strings);
        List<String> readableStrings = a.toString(ints);
        List<String> binaryStrings = a.toBinary(ints);

        Stringer w = new Stringer();
        return w.toMif(binaryStrings);
    }

    public ObjectId createAsm(String body) {
        //Asm asm = new Gson().fromJson(body, Asm.class);
        BasicDBObject doc = new BasicDBObject("asm", body).append("createdOn", new Date());
        assemblerasms.insert(doc);
        ObjectId id = (ObjectId)doc.get( "_id" );
        return id;
    }

    public Asm findAsm(String id) {
        return new Asm((BasicDBObject) assemblerasms.findOne(new BasicDBObject("_id", new ObjectId(id))));
    }

    public ObjectId createMif(String body) {
        BasicDBObject doc = new BasicDBObject("mif", body).append("createdOn", new Date());
        assemblermifs.insert(doc);
        ObjectId id = (ObjectId)doc.get( "_id" );
        return id;
    }

    public Mif findMif(String id) {
        return new Mif((BasicDBObject) assemblermifs.findOne(new BasicDBObject("_id", new ObjectId(id))));
    }
}
